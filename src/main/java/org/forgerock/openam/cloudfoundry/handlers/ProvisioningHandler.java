/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.cloudfoundry.handlers;

import static org.forgerock.http.protocol.Status.METHOD_NOT_ALLOWED;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.cloudfoundry.Responses.newEmptyResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.cloudfoundry.OpenAMClient;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles provision/deprovision operations against OpenAM.
 */
public class ProvisioningHandler implements Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningHandler.class);

    private final OpenAMClient openAMClient;

    /**
     * Constructs a new ProvisioningHandler.
     *
     * @param openAMClient The {@link OpenAMClient} used to communicate with OpenAM.
     */
    public ProvisioningHandler(OpenAMClient openAMClient) {
        this.openAMClient = openAMClient;
    }

    /**
     * Handles provisioning and deprovisioning operations.
     *
     * @param context The {@link Context}.
     * @param request The {@link Request}.
     * @return A {@link Promise} of a {@link Response}.
     */
    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        String instanceId = context.asContext(UriRouterContext.class).getUriTemplateVariables().get("instanceId");
        switch (request.getMethod()) {
        case "PUT":
        case "PATCH":
            LOGGER.info("Provisioning instance " + instanceId);
            return newResultPromise(newEmptyResponse(Status.OK).setEntity(json(object())));
        case "DELETE":
            return handleDelete(context, request, instanceId);
        default:
            return newResultPromise(newEmptyResponse(METHOD_NOT_ALLOWED));
        }
    }

    private Promise<Response, NeverThrowsException> handleDelete(Context context, Request request,
            final String instanceId) {
        LOGGER.info("Deprovisioning instance " + instanceId);
        return openAMClient.listClients().thenAsync(new AsyncFunction<Response, Response, NeverThrowsException>() {
            @Override
            public Promise<Response, NeverThrowsException> apply(Response response) throws NeverThrowsException {
                if (!response.getStatus().isSuccessful()) {
                    LOGGER.error("OpenAM returned an unexpected status (" + response.getStatus().getCode()
                            + ") retrieving client list for instance " + instanceId);
                    return newResultPromise(newEmptyResponse(Status.INTERNAL_SERVER_ERROR));
                }
                try {
                    return deleteClients(response, instanceId);
                } catch (IOException e) {
                    LOGGER.error("OpenAM returned unparsable body retrieving client list for instance " + instanceId);
                    return newResultPromise(newEmptyResponse(Status.INTERNAL_SERVER_ERROR));
                }
            }
        });
    }

    private Promise<Response, NeverThrowsException> deleteClients(Response response, final String instanceId)
            throws IOException {
        JsonValue json = json(response.getEntity().getJson());
        List<Promise<Response, NeverThrowsException>> deletionPromises = new ArrayList<>();
        for (Object result : json.get("result").asList()) {
            JsonValue r = json(result);
            String username = r.get("username").asString();
            if (username.startsWith(instanceId + "-")) {
                deletionPromises.add(openAMClient.deleteClient(username));
            }
        }
        return Promises.when(deletionPromises).then(new Function<List<Response>, Response, NeverThrowsException>() {
            @Override
            public Response apply(List<Response> responses) throws NeverThrowsException {
                for (Response response : responses) {
                    if (!response.getStatus().isSuccessful() && response.getStatus() != Status.BAD_REQUEST) {
                        LOGGER.error("OpenAM returned an unexpected status (" + response.getStatus().getCode()
                                + ") deleting client for instance " + instanceId);
                        return newEmptyResponse(Status.INTERNAL_SERVER_ERROR);
                    }
                }
                return newEmptyResponse(Status.OK);
            }
        });
    }
}
