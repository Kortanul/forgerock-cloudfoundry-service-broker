package org.forgerock.openam.cloudfoundry;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.util.promise.Promises.newResultPromise;

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

public class OpenAMClient {

    private final Client client = new Client(new HttpClientHandler());

    private final Configuration configuration;

    public OpenAMClient(Configuration configuration) throws HttpApplicationException {
        this.configuration = configuration;
    }

    public Promise<Response, NeverThrowsException> createClient(String authUsername, String authPassword, final String username, final String password) {
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

        return sendWithCredentials(authUsername, authPassword, createClientRequest);
    }

    public Promise<Response, NeverThrowsException> deleteClient(String authUsername, String authPassword, String username) {
        final Request request = new Request();
        request.setMethod("DELETE");
        request.setUri(configuration.getOpenAmApiBaseUrl().resolve("agents/" + username));
        return sendWithCredentials(authUsername, authPassword, request);
    }

    public Promise<Response, NeverThrowsException> listClients(String authUsername, String authPassword) {
        final Request queryClientRequest = new Request();
        queryClientRequest.setMethod("GET");
        queryClientRequest.setUri(configuration.getOpenAmApiBaseUrl().resolve("agents?_queryId=*"));

        return sendWithCredentials(authUsername, authPassword, queryClientRequest);
    }

    private Promise<String, AuthenticationFailedException> getOpenAmSession(String username, String password) {
        Request authenticateRequest = new Request();
        authenticateRequest.setMethod("POST");
        authenticateRequest.setUri(configuration.getOpenAmApiBaseUrl().resolve("authenticate?authTokenType=module&authIndexValue=Application"));
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

    private static List<String> orderedList(String... values) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            result.add("[" + i + "]=" + values[i]);
        }
        return result;
    }
}
