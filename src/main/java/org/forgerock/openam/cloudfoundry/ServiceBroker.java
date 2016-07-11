package org.forgerock.openam.cloudfoundry;

import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;
import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.openam.cloudfoundry.Responses.newEmptyResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.net.URISyntaxException;
import java.nio.charset.Charset;

import org.forgerock.guava.common.io.BaseEncoding;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.Router;
import org.forgerock.openam.cloudfoundry.handlers.BindingHandler;
import org.forgerock.openam.cloudfoundry.handlers.CatalogHandler;
import org.forgerock.openam.cloudfoundry.handlers.ProvisioningHandler;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

public class ServiceBroker {
    private final Router router = new Router();

    public ServiceBroker(Configuration configuration) throws HttpApplicationException, URISyntaxException {
        OpenAMClient client = new OpenAMClient(configuration);
        router.addRoute(requestUriMatcher(EQUALS, "/v2/catalog"), new CatalogHandler());
        router.addRoute(requestUriMatcher(EQUALS, "/v2/service_instances/{instanceId}"), new ProvisioningHandler(client));
        router.addRoute(requestUriMatcher(EQUALS, "/v2/service_instances/{instanceId}/service_bindings/{bindingId}"),
                new BindingHandler(client));
    }

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
            String[] authorizationTokenParts = authorizationTokenDecoded.split(":", 2);
            String username = authorizationTokenParts[0];
            String password = authorizationTokenParts[1];
            return new BasicAuthorizationContext(context, username, password);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationFailedException();
        }
    }
}
