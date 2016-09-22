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

import java.nio.charset.StandardCharsets;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.services.context.Context;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * A CHF filter for checking the request for an Authorization header with basic authentication against a fixed username
 * and password.
 */
public class AuthenticationFilter implements Filter {
    private final String expectedAuthorizationHeader;

    /**
     * Constructions a new authentication filter.
     * @param configuration the broker configuration.
     */
    public AuthenticationFilter(Configuration configuration) {
        String usernamePassword = configuration.getBrokerUsername() + ":" + configuration.getBrokerPassword();
        expectedAuthorizationHeader = "Basic " + Base64.encode(usernamePassword.getBytes(StandardCharsets.ISO_8859_1));
    }

    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
        if (isAuthenticated(request)) {
            return next.handle(context, request);
        } else {
            return Response.newResponsePromise(new Response(Status.UNAUTHORIZED));
        }
    }

    private boolean isAuthenticated(Request request) {
        Header header = request.getHeaders().get("Authorization");
        if (header == null) {
            return false;
        }
        if (header.getValues().size() > 1) {
            return false;
        }
        return header.getFirstValue().equals(expectedAuthorizationHeader);
    }
}
