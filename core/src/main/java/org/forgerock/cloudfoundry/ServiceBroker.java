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

import java.util.HashMap;
import java.util.Map;

import org.forgerock.cloudfoundry.handlers.ServiceBrokerHandler;
import org.forgerock.cloudfoundry.services.Service;
import org.forgerock.cloudfoundry.services.openam.OpenAMOAuth2Service;
import org.forgerock.http.HttpApplicationException;

/**
 * Root HTTP request handler for the service broker.
 *
 * <p>This is separate from {@link ServiceBrokerHandler} so that its dependencies can be injected by tests.</p>
 */
public class ServiceBroker {

    private final Map<String, Service> services = new HashMap<>();

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

        OpenAMClient openAMClient = new OpenAMClient(configuration);
        OpenAMOAuth2Service openAmoAuth2Service = new OpenAMOAuth2Service(openAMClient, pwGen);
        services.put(OpenAMOAuth2Service.SERVICE_ID, openAmoAuth2Service);
    }

    /**
     * Returns the services managed by this service broker.
     * @return  the services managed by this service broker.
     */
    public Map<String, Service> getServices() {
        return services;
    }

}
