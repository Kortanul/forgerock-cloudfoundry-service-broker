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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.cloudfoundry.handlers;

import static org.forgerock.cloudfoundry.Responses.newEmptyJsonResponse;
import static org.forgerock.http.protocol.Responses.newInternalServerError;
import static org.forgerock.http.protocol.Status.INTERNAL_SERVER_ERROR;
import static org.forgerock.http.protocol.Status.METHOD_NOT_ALLOWED;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.cloudfoundry.services.Service;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

abstract class BaseHandler implements Handler {

    private List<String> allowedMethods;
    private Map<String, Service> services;

    BaseHandler(List<String> allowedMethods, Map<String, Service> services) {
        this.allowedMethods = new ArrayList<>(allowedMethods);
        this.services = new HashMap<>(services);
    }

    @Override
    public final Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        String method = request.getMethod();
        if (!allowedMethods.contains(method)) {
            return newResultPromise(newEmptyJsonResponse(METHOD_NOT_ALLOWED));
        }

        try {
            String serviceId = new JsonValue(request.getEntity().getJson()).get("service_id").asString();
            Service service = services.get(serviceId);
            if (service != null) {
                return handle(context, request, service);
            }

            Response response = new Response(INTERNAL_SERVER_ERROR);
            response.getEntity().setJson(json(object(field("description", "Unknown service_id : " + serviceId))));
            return newResultPromise(response);
        } catch (IOException e) {
            return newResultPromise(newInternalServerError(e));
        }
    }

    protected abstract Promise<Response, NeverThrowsException> handle(Context context, Request request,
            Service service);
}

