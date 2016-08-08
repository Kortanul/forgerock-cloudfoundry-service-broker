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

package org.forgerock.openam.cloudfoundry.handlers;

import static org.forgerock.http.protocol.Status.METHOD_NOT_ALLOWED;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.cloudfoundry.Responses.newEmptyResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Handles Catalog calls from Cloud Foundry
 */
public class CatalogHandler implements Handler {

    private static final String SERVICE_ID = "3997be2d-e262-438e-8a31-8c90fa7156e5";
    private static final String PLAN_ID = "0140f6db-972a-466e-9e79-7845098a4ec7";

    /**
     * Handles catalog operations.
     *
     * @param context The {@link Context}.
     * @param request The {@link Request}.
     * @return A {@link Promise} of a {@link Response}.
     */
    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        if (!"GET".equals(request.getMethod())) {
            return newResultPromise(newEmptyResponse(METHOD_NOT_ALLOWED));
        }
        JsonValue result = json(object(field("services", array(object(
                    field("id", SERVICE_ID),
                field("name", "openam-oauth2"),
                field("description", "Uses ForgeRock OpenAM to provide OAuth 2.0 authorization"),
                field("tags", array("authentication", "oauth2")),
                field("bindable", true),
                field("metadata", object(
                        field("displayName", "ForgeRock OpenAM")
                )),
                field("plans", array(
                        object(
                                field("id", PLAN_ID),
                                field("name", "shared"),
                                field("description", "Shared OpenAM server")
                        )
                ))
        )))));
        return newResultPromise(newEmptyResponse(Status.OK).setEntity(result));
    }
}
