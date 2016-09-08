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
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.cloudfoundry.Responses.newEmptyResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;

import org.forgerock.openam.cloudfoundry.OpenAMClient;
import org.forgerock.openam.cloudfoundry.PasswordGenerator;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles binding/unbinding operations against OpenAM.
 */
public class BindingHandler implements Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BindingHandler.class);

    private final OpenAMClient openAMClient;
    private final PasswordGenerator passwordGenerator;

    /**
     * Constructs a new BindingHandler.
     *
     * @param openAMClient The {@link OpenAMClient} used to communicate with OpenAM.
     * @param passwordGenerator The {@link PasswordGenerator} used to generate OAuth2 Client passwords.
     */
    public BindingHandler(OpenAMClient openAMClient, PasswordGenerator passwordGenerator) {
        this.openAMClient = openAMClient;
        this.passwordGenerator = passwordGenerator;
    }

    /**
     * Handles binding and unbinding operations.
     *
     * @param context The {@link Context}.
     * @param request The {@link Request}.
     * @return A {@link Promise} of a {@link Response}.
     */
    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        String instanceId = context.asContext(UriRouterContext.class).getUriTemplateVariables().get("instanceId");
        String bindingId = context.asContext(UriRouterContext.class).getUriTemplateVariables().get("bindingId");

        switch (request.getMethod()) {
        case "PUT":
            return handleCreate(context, request, instanceId, bindingId);
        case "DELETE":
            return handleDelete(context, request, instanceId, bindingId);
        default:
            return newResultPromise(newEmptyResponse(METHOD_NOT_ALLOWED));
        }
    }

    private Promise<Response, NeverThrowsException> handleCreate(Context context, final Request request,
                                                                 final String instanceId, final String bindingId) {
        LOGGER.info("Creating binding for instance " + instanceId);
        try {
            JsonValue requestBody = json(request.getEntity().getJson());
            if (!requestBody.get("bind_resource").isDefined("app_guid")) {
                LOGGER.warn("Unable to create binding for instance " + instanceId
                        + ", app_guid is missing from bind_resource");
                return newResultPromise(newEmptyResponse(Status.valueOf(422, "Unprocessable Entity"))
                        .setEntity(json(object(
                            field("error", "RequiresApp"),
                            field("description", "This service supports generation of credentials through binding an "
                                    + "application only.")
                    ))));
            }

            final String username = instanceId + "-" + bindingId;
            final String password = passwordGenerator.generatePassword();

            return openAMClient.createClient(username, password)
                    .then(new Function<Response, Response, NeverThrowsException>() {
                        @Override
                        public Response apply(Response response) throws NeverThrowsException {
                            if (response.getStatus().isSuccessful()) {
                                return newEmptyResponse(Status.CREATED).setEntity(json(object(
                                        field("credentials", object(
                                                field("uri", openAMClient.getOAuth2Endpoint().toString()),
                                                field("username", username),
                                                field("password", password)
                                        ))
                                )));
                            } else if (response.getStatus() == Status.CONFLICT) {
                                LOGGER.warn("OpenAM already has a binding for " + instanceId + "-" + bindingId);
                                return newEmptyResponse(Status.CONFLICT);
                            } else {
                                LOGGER.error("OpenAM returned an unexpected status (" + response.getStatus().getCode()
                                        + ") creating binding " + instanceId + "-" + bindingId);
                                return newEmptyResponse(Status.INTERNAL_SERVER_ERROR);
                            }
                        }
                    });

        } catch (IOException e) {
            return newResultPromise(newEmptyResponse(Status.INTERNAL_SERVER_ERROR));
        }
    }

    private Promise<Response, NeverThrowsException> handleDelete(Context context, Request request,
            String instanceId, String bindingId) {
        final String username = instanceId + "-" + bindingId;
        LOGGER.info("Deleting binding " + bindingId + " for instance " + instanceId);
        return openAMClient.deleteClient(username).then(new Function<Response, Response, NeverThrowsException>() {
            @Override
            public Response apply(Response response) throws NeverThrowsException {
                if (response.getStatus().isSuccessful()) {
                    return newEmptyResponse(Status.OK);
                } else if (response.getStatus() == Status.BAD_REQUEST) {
                    LOGGER.warn("Binding " + username + " has already been removed");
                    return newEmptyResponse(Status.GONE);
                } else {
                    return response;
                }
            }
        });
    }
}
