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

package org.forgerock.cloudfoundry;

import static org.forgerock.cloudfoundry.Responses.newEmptyJsonResponse;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.http.Client;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for OpenAM related operations.
 */
public class OpenAMClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAMClient.class);

    private final Client client = new Client(new HttpClientHandler());
    private final Configuration configuration;
    private String cookieName;

    /**
     * Constructs a new OpenAM client.
     *
     * @param configuration The {@link Configuration} for the client.
     * @throws HttpApplicationException If the client is unable to determine the configured SSOToken cookie name.
     */
    public OpenAMClient(Configuration configuration) throws HttpApplicationException {
        this.configuration = configuration;
    }

    /**
     * Returns the OAuth2 base API URI.
     * @return Returns the OAuth2 base API URI.
     */
    public URI getOAuth2Endpoint() {
        return configuration.getOpenAmOAuth2Url();
    }

    /**
     * Creates an OAuth2 Client in OpenAM.
     *
     * @param username The username to use for the client.
     * @param password The password to use for the client.
     * @return A {@link Promise} containing the {@link Response} from OpenAM.
     */
    public Promise<Response, NeverThrowsException> createClient(final String username, final String password) {
        LOGGER.info("Creating OAuth2 client " + username);
        JsonValue responseBody = json(object(
                field("username", username),
                field("userpassword", password),
                field("AgentType", "OAuth2Client"),
                field("com.forgerock.openam.oauth2provider.name", orderedList(username)),
                field("com.forgerock.openam.oauth2provider.scopes",
                        orderedList(configuration.getScopes().toArray(new String[0])))
        ));
        final Request createClientRequest = new Request();
        createClientRequest.setEntity(responseBody);
        createClientRequest.setMethod("POST");
        createClientRequest.getHeaders().put("Accept-API-Version", "protocol=1.0, resource=3.0");
        createClientRequest.setUri(configuration.getOpenAmApiRealmUrl().resolve("agents?_action=create"));

        return sendWithCredentials(createClientRequest);
    }

    /**
     * Removes an OAuth2 client from OpenAM.
     *
     * @param username The username to remove.
     * @return A {@link Promise} containing the {@link Response} from OpenAM.
     */
    public Promise<Response, NeverThrowsException> deleteClient(String username) {
        LOGGER.info("Deleting OAuth2 client " + username);
        final Request request = new Request();
        request.setMethod("DELETE");
        request.getHeaders().put("Accept-API-Version", "protocol=1.0, resource=3.0");
        request.setUri(configuration.getOpenAmApiRealmUrl().resolve("agents/" + username));
        return sendWithCredentials(request);
    }

    /**
     * Returns a list of OAuth2 clients configured in OpenAM.
     * @return A {@link Promise} containing the {@link Response} from OpenAM.
     */
    public Promise<Response, NeverThrowsException> listClients() {
        LOGGER.info("Retrieving list of OAuth2 clients");
        final Request queryClientRequest = new Request();
        queryClientRequest.setMethod("GET");
        queryClientRequest.getHeaders().put("Accept-API-Version", "protocol=1.0, resource=3.0");
        queryClientRequest.setUri(configuration.getOpenAmApiRealmUrl().resolve("agents?_queryId=*"));

        return sendWithCredentials(queryClientRequest);
    }

    private Promise<Response, NeverThrowsException> getServerInfo() {
        URI serverInfoUri = configuration.getOpenAmApiBaseUrl().resolve("serverinfo/*");
        LOGGER.info("Retrieving OpenAM server info from " + serverInfoUri);
        Request request = new Request();
        request.setMethod("GET");
        request.getHeaders().put("Accept-API-Version", "protocol=1.0, resource=1.1");
        request.setUri(serverInfoUri);
        return client.send(request);
    }

    private Promise<String, HttpApplicationException> getCookieName() {
        LOGGER.info("Determining OpenAM SSO token cookie name");
        if (cookieName != null) {
            return newResultPromise(cookieName);
        } else {
            return getServerInfo().then(new Function<Response, String, HttpApplicationException>() {
                @Override
                public String apply(Response response) throws HttpApplicationException {
                    try {
                        cookieName = json(response.getEntity().getJson()).get("cookieName").asString();
                        if (cookieName != null) {
                            LOGGER.info("SSO token cookie name is " + cookieName);
                            return cookieName;
                        } else {
                            throw new HttpApplicationException("Unable to resolve cookie name");
                        }
                    } catch (IOException e) {
                        throw new HttpApplicationException("Unable to resolve cookie name");
                    }
                }
            }, new Function<NeverThrowsException, String, HttpApplicationException>() {
                @Override
                public String apply(NeverThrowsException value) throws HttpApplicationException {
                    throw new HttpApplicationException("Unable to resolve cookie name");
                }
            });
        }
    }

    private Promise<String, AuthenticationFailedException> getOpenAmSession(String username, String password) {
        URI authenticateUri = configuration.getOpenAmApiRealmUrl()
                .resolve("authenticate?authTokenType=module&authIndexValue=Application");
        LOGGER.info("Creating OpenAM SSO token from " + authenticateUri);
        Request authenticateRequest = new Request();
        authenticateRequest.setMethod("POST");
        authenticateRequest.setUri(authenticateUri);
        authenticateRequest.getHeaders().put("X-OpenAM-Username", username);
        authenticateRequest.getHeaders().put("X-OpenAM-Password", password);
        return client.send(authenticateRequest).then(new Function<Response, String, AuthenticationFailedException>() {
            @Override
            public String apply(Response response) throws AuthenticationFailedException {
                if (!response.getStatus().isSuccessful()) {
                    LOGGER.warn("Unable to authenticate against OpenAM");
                    LOGGER.warn("OpenAM response: " + response.getEntity().toString());
                    throw new AuthenticationFailedException();
                }
                try {
                    LOGGER.info("Authentication successful");
                    return json(response.getEntity().getJson()).get("tokenId").asString();
                } catch (Exception e) {
                    LOGGER.warn("Unable to extract SSO token");
                    throw new AuthenticationFailedException();
                }
            }
        }, null);
    }

    private Promise<Response, NeverThrowsException> sendWithCredentials(final Request request) {
        return getOpenAmSession(configuration.getOpenAmUsername(), configuration.getOpenAmPassword())
                .thenAsync(new AsyncFunction<String, Response, NeverThrowsException>() {
                    @Override
                    public Promise<Response, NeverThrowsException> apply(final String token)
                            throws NeverThrowsException {
                        return getCookieName().thenAsync(new AsyncFunction<String, Response, NeverThrowsException>() {
                            @Override
                            public Promise<? extends Response, ? extends NeverThrowsException> apply(String cookieName)
                                    throws NeverThrowsException {
                                request.getHeaders().add(cookieName, token);
                                return client.send(request);
                            }
                        }, new AsyncFunction<HttpApplicationException, Response, NeverThrowsException>() {
                            @Override
                            public Promise<Response, NeverThrowsException> apply(HttpApplicationException exception)
                                    throws NeverThrowsException {
                                return newResultPromise(newEmptyJsonResponse(Status.INTERNAL_SERVER_ERROR));
                            }
                        });
                    }
                }, new AsyncFunction<AuthenticationFailedException, Response, NeverThrowsException>() {
                    @Override
                    public Promise<Response, NeverThrowsException> apply(AuthenticationFailedException exception)
                            throws NeverThrowsException {
                        return newResultPromise(newEmptyJsonResponse(Status.UNAUTHORIZED));
                    }
                });
    }

    private static List<String> orderedList(String... values) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            result.add("[" + i + "]=" + values[i]);
        }
        return result;
    }
}
