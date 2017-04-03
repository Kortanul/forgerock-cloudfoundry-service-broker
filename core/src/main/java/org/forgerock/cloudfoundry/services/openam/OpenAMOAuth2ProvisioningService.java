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

package org.forgerock.cloudfoundry.services.openam;

import static org.forgerock.cloudfoundry.Responses.newEmptyJsonResponse;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.cloudfoundry.OpenAMClient;
import org.forgerock.cloudfoundry.services.ProvisioningService;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
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
class OpenAMOAuth2ProvisioningService implements ProvisioningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAMOAuth2ProvisioningService.class);

    private final OpenAMClient openAMClient;

    /**
     * Constructs a new ProvisioningHandler.
     *
     * @param openAMClient The {@link OpenAMClient} used to communicate with OpenAM.
     */
    public OpenAMOAuth2ProvisioningService(OpenAMClient openAMClient) {
        this.openAMClient = openAMClient;
    }

    @Override
    public Promise<Response, NeverThrowsException> provision(String instanceId, JsonValue parameters) {
        LOGGER.info("Provisioning instance " + instanceId);
        return newResultPromise(newEmptyJsonResponse(Status.OK));
    }

    @Override
    public Promise<Response, NeverThrowsException> update(String instanceId, JsonValue parameters) {
        LOGGER.info("Updating instance " + instanceId);
        return newResultPromise(newEmptyJsonResponse(Status.OK));
    }

    @Override
    public Promise<Response, NeverThrowsException> deprovision(final String instanceId) {
        LOGGER.info("Deprovisioning instance " + instanceId);
        return openAMClient.listClients().thenAsync(new AsyncFunction<Response, Response, NeverThrowsException>() {
            @Override
            public Promise<Response, NeverThrowsException> apply(Response response) throws NeverThrowsException {
                if (!response.getStatus().isSuccessful()) {
                    LOGGER.error("OpenAM returned an unexpected status (" + response.getStatus().getCode()
                            + ") retrieving client list for instance " + instanceId);
                    return newResultPromise(newEmptyJsonResponse(Status.INTERNAL_SERVER_ERROR));
                }
                try {
                    return deleteClients(response, instanceId);
                } catch (IOException e) {
                    LOGGER.error("OpenAM returned unparsable body retrieving client list for instance " + instanceId);
                    return newResultPromise(newEmptyJsonResponse(Status.INTERNAL_SERVER_ERROR));
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
                        return newEmptyJsonResponse(Status.INTERNAL_SERVER_ERROR);
                    }
                }
                return newEmptyJsonResponse(Status.OK);
            }
        });
    }

}
