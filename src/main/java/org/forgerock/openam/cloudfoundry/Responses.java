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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;

/**
 * Utility class for Responses
 */
public final class Responses {

    private Responses() { }

    /**
     * Constructs a new empty response.
     *
     * @param status The {@link Status} of the response.
     * @return An empty {@link Response} with the specified {@link Status}.
     */
    public static Response newEmptyResponse(Status status) {
        return new Response(status).setEntity(json(object()));
    }
}
