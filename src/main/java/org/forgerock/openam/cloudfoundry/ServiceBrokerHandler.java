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

package org.forgerock.openam.cloudfoundry;

import static org.forgerock.http.protocol.Status.METHOD_NOT_ALLOWED;
import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;
import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.guava.common.io.BaseEncoding;
import org.forgerock.http.Client;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.Router;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

class ServiceBrokerHandler implements Handler {
    private final Client client = new Client(new HttpClientHandler());
    private final Router router = new Router();
    private final Configuration configuration = new Configuration();

    public ServiceBrokerHandler() throws HttpApplicationException, URISyntaxException {
        router.addRoute(requestUriMatcher(EQUALS, "/v2/catalog"), new CatalogHandler());
        router.addRoute(requestUriMatcher(EQUALS, "/v2/service_instances/{instanceId}"), new ProvisioningHandler());
        router.addRoute(requestUriMatcher(EQUALS, "/v2/service_instances/{instanceId}/service_bindings/{bindingId}"),
                new BindingHandler());
    }

    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        try {
            context = getAuthContext(context, request);
        } catch (AuthenticationFailedException e) {
            return newResultPromise(newEmptyResponse(Status.UNAUTHORIZED));
        }
        return router.handle(context, request);
    }

    private Context getAuthContext(Context context, Request request) throws AuthenticationFailedException {
        String authorization = request.getHeaders().getFirst("Authorization");
        String basicAuthPrefix = "Basic ";
        if (authorization == null || !authorization.startsWith(basicAuthPrefix)) {
            throw new AuthenticationFailedException();
        }
        String authorizationToken = authorization.substring(basicAuthPrefix.length());
        try {
            String authorizationTokenDecoded = new String(BaseEncoding.base64().decode(authorizationToken), Charset.forName("UTF-8"));
            String[] authorizationTokenParts = authorizationTokenDecoded.split(":");
            if (authorizationTokenParts.length != 2) {
                throw new AuthenticationFailedException();
            }
            String username = authorizationTokenParts[0];
            String password = authorizationTokenParts[1];
            return new BasicAuthorizationContext(context, username, password);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationFailedException();
        }
    }

    private static class CatalogHandler implements Handler {
        @Override
        public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
            if (!"GET".equals(request.getMethod())) {
                return newResultPromise(newEmptyResponse(METHOD_NOT_ALLOWED));
            }
            JsonValue result = json(object(field("services", array(object(
                    field("id", "3997be2d-e262-438e-8a31-8c90fa7156e5"),
                    field("name", "openam-oauth2"),
                    field("description", "Uses ForgeRock OpenAM to provide OAuth 2.0 authorization"),
                    field("tags", array("authentication", "oauth2")),
                    field("bindable", true),
                    field("metadata", object(
                            field("displayName", "ForgeRock OpenAM")
                    )),
                    field("plans", array(
                            object(
                                    field("id", "0140f6db-972a-466e-9e79-7845098a4ec7"),
                                    field("name", "shared"),
                                    field("description", "Shared OpenAM server")
                            )
                    ))
            )))));
            return newResultPromise(newEmptyResponse(Status.OK).setEntity(result));
        }
    }

    private class ProvisioningHandler implements Handler {
        @Override
        public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
            String instanceId = context.asContext(UriRouterContext.class).getUriTemplateVariables().get("instanceId");
            if ("PUT".equals(request.getMethod())) {
                return handleCreate(context, request, instanceId);
            } else if ("PATCH".equals(request.getMethod())) {
                return handleUpdate(context, request, instanceId);
            } else if ("DELETE".equals(request.getMethod())) {
                return handleDelete(context, request, instanceId);
            }
            return newResultPromise(newEmptyResponse(METHOD_NOT_ALLOWED));
        }

        private Promise<Response, NeverThrowsException> handleCreate(Context context, Request request,
                String instanceId) {
            return newResultPromise(newEmptyResponse(Status.OK));
        }

        private Promise<Response, NeverThrowsException> handleUpdate(Context context, Request request,
                String instanceId) {
            return newResultPromise(newEmptyResponse(Status.OK));
        }

        private Promise<Response, NeverThrowsException> handleDelete(Context context, Request request,
                String instanceId) {
            final Request queryClientRequest = new Request();
            queryClientRequest.setMethod("GET");
            queryClientRequest.setUri(configuration.getOpenAmApiBaseUrl().resolve("agents?_queryId=*"));
            BasicAuthorizationContext authContext = context.asContext(BasicAuthorizationContext.class);
            return sendWithCredentials(authContext.getUsername(), authContext.getPassword(), request).then(new Function<Response, Response, NeverThrowsException>() {
                @Override
                public Response apply(Response response) throws NeverThrowsException {
                    if (response.getStatus().isSuccessful()) {
                        try {
                            JsonValue json = json(response.getEntity().getJson());
                            json.get("results");
                        } catch (IOException e) {
                            return newEmptyResponse(Status.INTERNAL_SERVER_ERROR);
                        }
                    } else if (response.getStatus() == Status.CONFLICT) {
                        return newEmptyResponse(Status.CONFLICT);
                    }
                    return response;
                }
            });
        }
    }

    private class BindingHandler implements Handler {
        @Override
        public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
            String instanceId = context.asContext(UriRouterContext.class).getUriTemplateVariables().get("instanceId");
            String bindingId = context.asContext(UriRouterContext.class).getUriTemplateVariables().get("bindingId");
            if ("PUT".equals(request.getMethod())) {
                return handleCreate(context, request, instanceId, bindingId);
            } else if ("DELETE".equals(request.getMethod())) {
                return handleDelete(context, request, instanceId, bindingId);
            }
            return newResultPromise(newEmptyResponse(METHOD_NOT_ALLOWED));
        }

        private Promise<Response, NeverThrowsException> handleCreate(Context context, Request request,
                String instanceId, String bindingId) {
            try {
                JsonValue requestBody = json(request.getEntity().getJson());
                if (!requestBody.get("bind_resource").isDefined("app_guid")) {
                    return newResultPromise(newEmptyResponse(Status.valueOf(422, "Unprocessable Entity")).setEntity(json(object(
                            field("error", "RequiresApp"),
                            field("description", "This service supports generation of credentials through binding an application only.")
                    ))));
                }

                final String username = instanceId + "-" + bindingId;
                final String password = "foo"; // TODO: Generate securely
                JsonValue responseBody = json(object(
                        field("username", username),
                        field("userpassword", password),
                        field("AgentType", "OAuth2Client"),
                        field("com.forgerock.openam.oauth2provider.name", orderedList(username))
                        //field("com.forgerock.openam.oauth2provider.redirectionURIs", orderedList(redirectionUris))
                ));
                final Request createClientRequest = new Request();
                createClientRequest.setEntity(responseBody);
                createClientRequest.setMethod("POST");
                createClientRequest.setUri(configuration.getOpenAmApiBaseUrl().resolve("agents?_action=create"));
                BasicAuthorizationContext authContext = context.asContext(BasicAuthorizationContext.class);
                return sendWithCredentials(authContext.getUsername(), authContext.getPassword(), createClientRequest).then(new Function<Response, Response, NeverThrowsException>() {
                    @Override
                    public Response apply(Response response) throws NeverThrowsException {
                        if (response.getStatus().isSuccessful()) {
                            return newEmptyResponse(Status.CREATED).setEntity(json(object(
                                    field("credentials", object(
                                            field("username", username),
                                            field("password", password)
                                    ))
                            )));
                        } else if (response.getStatus() == Status.CONFLICT) {
                            return newEmptyResponse(Status.CONFLICT);
                        } else {
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
            final Request createClientRequest = new Request();
            createClientRequest.setMethod("DELETE");
            createClientRequest.setUri(configuration.getOpenAmApiBaseUrl().resolve("agents/" + username));
            BasicAuthorizationContext authContext = context.asContext(BasicAuthorizationContext.class);
            return sendWithCredentials(authContext.getUsername(), authContext.getPassword(), request).then(new Function<Response, Response, NeverThrowsException>() {
                @Override
                public Response apply(Response response) throws NeverThrowsException {
                    if (response.getStatus().isSuccessful()) {
                        return newEmptyResponse(Status.CREATED);
                    } else if (response.getStatus() == Status.BAD_REQUEST) { // TODO: Is this a good assumption?
                        return newEmptyResponse(Status.GONE);
                    }
                    return response;
                }
            });
        }
    }

    private Promise<String, AuthenticationFailedException> getOpenAmSession(String username, String password) {
        Request authenticateRequest = new Request();
        authenticateRequest.setMethod("POST");
        authenticateRequest.setUri(configuration.getOpenAmApiBaseUrl().resolve("authenticate"));
        authenticateRequest.getHeaders().put("X-OpenAM-Username", username);
        authenticateRequest.getHeaders().put("X-OpenAM-Password", password);
        return client.send(authenticateRequest).then(new Function<Response, String, AuthenticationFailedException>() {
            @Override
            public String apply(Response response) throws AuthenticationFailedException {
                if (!response.getStatus().isSuccessful()) {
                    throw new AuthenticationFailedException();
                }
                try {
                    return json(response.getEntity().getJson()).get("tokenId").asString();
                } catch (Exception e) {
                    throw new AuthenticationFailedException();
                }
            }
        }, null);
    }

    private static List<String> orderedList(String... values) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            result.add("[" + i + "]=" + values[i]);
        }
        return result;
    }

    private Promise<Response, NeverThrowsException> sendWithCredentials(String username, String password, final Request request) {
        return getOpenAmSession(username, password).thenAsync(new AsyncFunction<String, Response, NeverThrowsException>() {
            @Override
            public Promise<Response, NeverThrowsException> apply(String token) throws NeverThrowsException {
                String headerName = "iPlanetDirectoryPro"; // TODO: Get this via config
                request.getHeaders().put(headerName, token);
                return client.send(request);
            }
        }, new AsyncFunction<AuthenticationFailedException, Response, NeverThrowsException>() {
            @Override
            public Promise<Response, NeverThrowsException> apply(AuthenticationFailedException exception) throws NeverThrowsException {
                return newResultPromise(newEmptyResponse(Status.UNAUTHORIZED));
            }
        });
    }

    private static Response newEmptyResponse(Status status) {
        return new Response(status).setEntity(json(object()));
    }
}
