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
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.cloudfoundry.OpenAMClient;
import org.forgerock.cloudfoundry.PasswordGenerator;
import org.forgerock.cloudfoundry.services.BindingService;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles binding/unbinding operations against OpenAM.
 */
class OpenAMOAuth2BindingService implements BindingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAMOAuth2BindingService.class);

    private final OpenAMClient openAMClient;
    private final PasswordGenerator passwordGenerator;

    /**
     * Constructs a new BindingHandler.
     *
     * @param openAMClient The {@link OpenAMClient} used to communicate with OpenAM.
     * @param passwordGenerator The {@link PasswordGenerator} used to generate OAuth2 Client passwords.
     */
    public OpenAMOAuth2BindingService(OpenAMClient openAMClient, PasswordGenerator passwordGenerator) {
        this.openAMClient = openAMClient;
        this.passwordGenerator = passwordGenerator;
    }

    @Override
    public Promise<Response, NeverThrowsException> bind(final String instanceId, final String bindingId,
            final JsonValue bindResource, final JsonValue parameters) {
        LOGGER.info("Creating binding for instance " + instanceId);
        if (!bindResource.isDefined("app_guid")) {
            LOGGER.warn("Unable to create binding for instance " + instanceId
                    + ", app_guid is missing from bind_resource");
            return newResultPromise(new Response(Status.valueOf(422, "Unprocessable Entity"))
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
                        Status status = response.getStatus();
                        if (status.isSuccessful()) {
                            return new Response(Status.CREATED).setEntity(json(object(
                                    field("credentials", object(
                                            field("uri", openAMClient.getOAuth2Endpoint().toString()),
                                            field("username", username),
                                            field("password", password)
                                    ))
                            )));
                        } else if (status == Status.CONFLICT) {
                            LOGGER.warn("OpenAM already has a binding for " + username);
                            return newEmptyJsonResponse(Status.CONFLICT);
                        } else {
                            LOGGER.error("OpenAM returned an unexpected status (" + status.getCode() + ") "
                                    + "creating binding " + username);
                            return newEmptyJsonResponse(Status.INTERNAL_SERVER_ERROR);
                        }
                    }
                });
    }

    @Override
    public Promise<Response, NeverThrowsException> unbind(String instanceId, String bindingId) {
        final String username = instanceId + "-" + bindingId;
        LOGGER.info("Deleting binding " + bindingId + " for instance " + instanceId);
        return openAMClient.deleteClient(username).then(new Function<Response, Response, NeverThrowsException>() {
            @Override
            public Response apply(Response response) throws NeverThrowsException {
                if (response.getStatus().isSuccessful()) {
                    return newEmptyJsonResponse(Status.OK);
                } else if (response.getStatus() == Status.BAD_REQUEST) {
                    LOGGER.warn("Binding " + username + " has already been removed");
                    return newEmptyJsonResponse(Status.GONE);
                } else {
                    return response;
                }
            }
        });
    }
}
