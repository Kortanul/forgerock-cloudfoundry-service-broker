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

import static org.forgerock.http.handler.Handlers.filtered;
import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;
import static org.forgerock.http.routing.RoutingMode.EQUALS;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.cloudfoundry.handlers.BindingHandler;
import org.forgerock.cloudfoundry.handlers.CatalogHandler;
import org.forgerock.cloudfoundry.handlers.ProvisioningHandler;
import org.forgerock.cloudfoundry.handlers.ServiceBrokerHandler;
import org.forgerock.cloudfoundry.services.Service;
import org.forgerock.cloudfoundry.services.openam.OpenAMOAuth2Service;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.routing.Router;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Root HTTP request handler for the service broker.
 *
 * <p>This is separate from {@link ServiceBrokerHandler} so that its dependencies can be injected by tests.</p>
 */
public class ServiceBroker {

    private final Handler handler;

    /**
     * Constructs a new ServiceBroker.
     *
     * @param configuration The {@link Configuration} for the underlying {@link OpenAMClient}.
     * @param pwGen The {@link PasswordGenerator} used to generate client passwords.
     * @throws HttpApplicationException if the underlying {@link OpenAMClient} throws a
     *                                  {@link HttpApplicationException}.
     */
    public ServiceBroker(Configuration configuration, PasswordGenerator pwGen)
            throws HttpApplicationException {
        Map<String, Service> services = new HashMap<>();

        OpenAMClient openAMClient = new OpenAMClient(configuration);
        OpenAMOAuth2Service openAmoAuth2Service = new OpenAMOAuth2Service(openAMClient, pwGen);
        services.put(OpenAMOAuth2Service.SERVICE_ID, openAmoAuth2Service);

        Router router = new Router();
        router.addRoute(requestUriMatcher(EQUALS, "/v2/catalog"),
                new CatalogHandler(services.values()));
        router.addRoute(requestUriMatcher(EQUALS, "/v2/service_instances/{instanceId}"),
                new ProvisioningHandler(services));
        router.addRoute(requestUriMatcher(EQUALS, "/v2/service_instances/{instanceId}/service_bindings/{bindingId}"),
                new BindingHandler(services));

        handler = filtered(router, new AuthenticationFilter(configuration));
    }

    /**
     * Handle the incoming {@link Request}.
     *
     * @param context The {@link Context}.
     * @param request The {@link Request}.
     * @return A {@link Promise} of a {@link Response}.
     */
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        return handler.handle(context, request);
    }
}
