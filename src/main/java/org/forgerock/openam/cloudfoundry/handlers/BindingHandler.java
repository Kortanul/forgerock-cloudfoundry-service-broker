package org.forgerock.openam.cloudfoundry.handlers;

import static org.forgerock.http.protocol.Status.METHOD_NOT_ALLOWED;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.cloudfoundry.Responses.newEmptyResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;

import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.cloudfoundry.BasicAuthorizationContext;
import org.forgerock.openam.cloudfoundry.OpenAMClient;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

public class BindingHandler implements Handler {
    private final OpenAMClient openAMClient;

    public BindingHandler(OpenAMClient openAMClient) {
        this.openAMClient = openAMClient;
    }

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

            BasicAuthorizationContext authContext = context.asContext(BasicAuthorizationContext.class);

            final String username = instanceId + "-" + bindingId;
            final String password = "foo"; // TODO: Generate securely

            return openAMClient.createClient(authContext.getUsername(), authContext.getPassword(), username, password).then(new Function<Response, Response, NeverThrowsException>() {
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
        BasicAuthorizationContext authContext = context.asContext(BasicAuthorizationContext.class);
        final String username = instanceId + "-" + bindingId;
        return openAMClient.deleteClient(authContext.getUsername(), authContext.getPassword(), username).then(new Function<Response, Response, NeverThrowsException>() {
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
