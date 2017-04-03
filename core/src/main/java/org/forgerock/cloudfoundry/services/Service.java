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

package org.forgerock.cloudfoundry.services;

import org.forgerock.json.JsonValue;

/**
 * Define the contract that a Service exposed by our Service Broker has to implement.
 */
public interface Service {

    /**
     * Returns the Json containing all the service's metadata.
     * @return the Json containing all the service's metadata.
     */
    JsonValue getServiceMetadata();

    /**
     * Returns the {@link ProvisioningService} used to manage the provisioning operations.
     * @return the {@link ProvisioningService} used to manage the provisioning operations.
     */
    ProvisioningService getProvisioningService();

    /**
     * Returns the {@link BindingService} used to manage the binding operations.
     * @return the {@link BindingService} used to manage the binding operations.
     */
    BindingService getBindingService();
}

