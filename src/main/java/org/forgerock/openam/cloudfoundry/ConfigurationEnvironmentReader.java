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

/**
 * Reads {@link Configuration} properties from the system environment properties.
 */
public class ConfigurationEnvironmentReader {

    /**
     * Read the system environment properties into a {@link Configuration} object.
     * @return A {@link Configuration} object.
     */
    public Configuration read() {
        return new Configuration(
                System.getenv("OPENAM_BASE_URI"),
                System.getenv("OPENAM_USERNAME"),
                System.getenv("OPENAM_PASSWORD"),
                System.getenv("OPENAM_REALM"));
    }
}
