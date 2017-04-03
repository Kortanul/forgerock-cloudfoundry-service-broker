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

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;

/**
 * Utility class for Responses.
 */
public final class Responses {

    private Responses() { }

    /**
     * Constructs a new empty response filled with an empty Json object.
     *
     * @param status The {@link Status} of the response.
     * @return An empty {@link Response} with the specified {@link Status}.
     */
    public static Response newEmptyJsonResponse(Status status) {
        return new Response(status).setEntity(json(object()));
    }

    /**
     * Constructs a new response filled with a Json object carrying a description about the error.
     *
     * @param status The {@link Status} of the response.
     * @param description A description of the error.
     * @return An {@link Response} with the specified {@link Status} containing a Json object filled
     * with a description about the error.
     */
    public static Response newErrorJsonResponse(Status status, String description) {
        return new Response(status).setEntity(json(object(field("description", description))));
    }

    /**
     * Constructs a new response filled with a Json object carrying a description about the error.
     *
     * @param status The {@link Status} of the response.
     * @param exception The exception that caused an error.
     * @return An {@link Response} with the specified {@link Status} containing a Json object filled
     * with a description about the error.
     */
    public static Response newErrorJsonResponse(Status status, Exception exception) {
        return newErrorJsonResponse(status, exception, exception.getMessage());
    }

    /**
     * Constructs a new response filled with a Json object carrying a description about the error.
     *
     * @param status The {@link Status} of the response.
     * @param exception The exception that caused an error.
     * @param description A description of the error.
     * @return An {@link Response} with the specified {@link Status} containing a Json object filled
     * with a description about the error.
     */
    public static Response newErrorJsonResponse(Status status, Exception exception, String description) {
        return new Response(status).setCause(exception).setEntity(json(object(field("description", description))));
    }

}
