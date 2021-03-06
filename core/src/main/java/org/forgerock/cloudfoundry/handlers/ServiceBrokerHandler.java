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

package org.forgerock.cloudfoundry.handlers;

import static org.forgerock.http.handler.Handlers.chainOf;
import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;
import static org.forgerock.http.routing.RoutingMode.EQUALS;

import java.util.Map;

import org.forgerock.cloudfoundry.AuthenticationFilter;
import org.forgerock.cloudfoundry.ServiceBroker;
import org.forgerock.cloudfoundry.services.Service;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.routing.Router;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Root HTTP request handler for the service broker.
 *
 * <p>Delegates to {@link ServiceBroker}.</p>
 */
public class ServiceBrokerHandler implements Handler {

    private final Handler handler;

    /**
     * Construct a new ServiceBrokerHandler.
     * @param services the service to manage.
     * @param brokerUsername the username to access this service broker.
     * @param brokerPassword the password to access this service broker.
     */
    public ServiceBrokerHandler(Map<String, Service> services, String brokerUsername, String brokerPassword) {
        Router router = new Router();
        router.addRoute(requestUriMatcher(EQUALS, "/v2/catalog"),
                new CatalogHandler(services.values()));
        router.addRoute(requestUriMatcher(EQUALS, "/v2/service_instances/{instanceId}"),
                new ProvisioningHandler(services));
        router.addRoute(requestUriMatcher(EQUALS, "/v2/service_instances/{instanceId}/service_bindings/{bindingId}"),
                new BindingHandler(services));

        handler = chainOf(router, new AuthenticationFilter(brokerUsername, brokerPassword));
    }

    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        return handler.handle(context, request);
    }

}
