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

import org.forgerock.openam.cloudfoundry.ConfigurationEnvironmentReader;
import org.forgerock.openam.cloudfoundry.OpenAMClient;
import org.forgerock.openam.cloudfoundry.PasswordGenerator;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.openam.cloudfoundry.ServiceBroker;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Root HTTP request handler for the service broker.
 *
 * <p>Delegates to {@link ServiceBroker}.</p>
 */
public class ServiceBrokerHandler implements Handler {

    private final ServiceBroker broker = new ServiceBroker(new ConfigurationEnvironmentReader().read(),
            new PasswordGenerator());

    /**
     * Constructs new ServiceBrokerHandler.
     *
     * @throws HttpApplicationException if underlying {@link OpenAMClient} throws a {@link HttpApplicationException}.
     */
    public ServiceBrokerHandler() throws HttpApplicationException { }

    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        return broker.handle(context, request);
    }
}
