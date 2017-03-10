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

import static org.forgerock.cloudfoundry.Responses.newEmptyJsonResponse;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.List;

import org.forgerock.cloudfoundry.services.Service;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Handles Catalog calls from Cloud Foundry.
 */
public class CatalogHandler implements Handler {

    private final Iterable<Service> services;

    /**
     * Constructs a new CatalogHandler.
     *
     * @param services The services that will be exposed by this catalog.
     */
    public CatalogHandler(Iterable<Service> services) {
        this.services = services;
    }

    /**
     * Handles catalog operations.
     *
     * @param context The {@link Context}.
     * @param request The {@link Request}.
     * @return A {@link Promise} of a {@link Response}.
     */
    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        // Only GET is allowed
        if (!"GET".equals(request.getMethod())) {
            return newResultPromise(newEmptyJsonResponse(Status.METHOD_NOT_ALLOWED));
        }

        List<Object> arrayServices = array();
        for (Service service : services) {
            arrayServices.add(service.getServiceMetadata());
        }
        JsonValue result = json(object(field("services", arrayServices)));

        return newResultPromise(new Response(Status.OK).setEntity(result));
    }
}
