package org.forgerock.openam.cloudfoundry.handlers;

import static org.forgerock.http.protocol.Status.METHOD_NOT_ALLOWED;
import static org.forgerock.json.JsonValue.json;
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
import org.forgerock.openam.cloudfoundry.BasicAuthorizationContext;
import org.forgerock.openam.cloudfoundry.OpenAMClient;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

public class ProvisioningHandler implements Handler {
    private final OpenAMClient openAMClient;

    public ProvisioningHandler(OpenAMClient openAMClient) {
        this.openAMClient = openAMClient;
    }

    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        String instanceId = context.asContext(UriRouterContext.class).getUriTemplateVariables().get("instanceId");
        switch (request.getMethod()) {
            case "PUT":
            case "PATCH":
                return newResultPromise(newEmptyResponse(Status.OK));
            case "DELETE":
                return handleDelete(context, request, instanceId);
            default:
                return newResultPromise(newEmptyResponse(METHOD_NOT_ALLOWED));
        }
    }

    private Promise<Response, NeverThrowsException> handleDelete(Context context, Request request,
            final String instanceId) {
        final BasicAuthorizationContext authContext = context.asContext(BasicAuthorizationContext.class);
        return openAMClient.listClients(authContext.getUsername(), authContext.getPassword()).thenAsync(new AsyncFunction<Response, Response, NeverThrowsException>() {
            @Override
            public Promise<Response, NeverThrowsException> apply(Response response) throws NeverThrowsException {
                if (!response.getStatus().isSuccessful()) {
                    return newResultPromise(newEmptyResponse(Status.INTERNAL_SERVER_ERROR));
                }
                try {
                    JsonValue json = json(response.getEntity().getJson());
                    List<Promise<Response, NeverThrowsException>> deletionPromises = new ArrayList<>();
                    for (Object result : json.get("result").asList()) {
                        JsonValue r = json(result);
                        String username = r.get("username").asString();
                        if (username.startsWith(instanceId + "-")) {
                            deletionPromises.add(openAMClient.deleteClient(authContext.getUsername(), authContext.getPassword(), username));
                        }
                    }
                    return Promises.when(deletionPromises).then(new Function<List<Response>, Response, NeverThrowsException>() {
                        @Override
                        public Response apply(List<Response> responses) throws NeverThrowsException {
                            for (Response response : responses) {
                                if (!response.getStatus().isSuccessful()) {
                                    return newEmptyResponse(Status.INTERNAL_SERVER_ERROR);
                                }
                            }
                            return newEmptyResponse(Status.OK);
                        }
                    });
                } catch (IOException e) {
                    return newResultPromise(newEmptyResponse(Status.INTERNAL_SERVER_ERROR));
                }
            }
        });
    }
}
