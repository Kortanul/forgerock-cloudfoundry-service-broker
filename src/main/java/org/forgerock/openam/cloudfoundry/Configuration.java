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

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.cloudfoundry.client.OpenAMClient;

/**
 * Configuration for {@link OpenAMClient}
 */
public class Configuration {

    private static final String ROOT_REALM = "/";

    private final URI openAmApiBaseUrl;
    private final URI openAmApiRealmUrl;
    private final String username;
    private final String password;

    /**
     * Constructor
     *
     * @param baseUri
     * The base URI of OpenAM
     * @param username
     * The username to use to authenticate against OpenAM
     * @param password
     * The password to use to authenticate against OpenAM
     * @param realm
     * The OpenAM realm to use
     * @throws URISyntaxException if the OpenAM base URI is not a valid URI
     */
    public Configuration(String baseUri, String username, String password, String realm) {

        try {
            openAmApiBaseUrl = new URI(validateProperty(baseUri, "OPENAM_BASE_URI") + "/").resolve("json/");
        } catch (URISyntaxException e) {
            throw new IllegalStateException("OPENAM_BASE_URI is not a valid URI", e);
        }

        this.username = validateProperty(username, "OPENAM_USERNAME");
        this.password = validateProperty(password, "OPENAM_PASSWORD");

        if (StringUtils.trimToNull(realm) == null || realm.equals(ROOT_REALM)) {
            openAmApiRealmUrl = openAmApiBaseUrl;
        } else {
            openAmApiRealmUrl = openAmApiBaseUrl.resolve(realm.replaceFirst("^/", "") + "/");
        }

    }

    /**
     * Returns the OpenAM base URI
     * @return
     * The OpenAM base URI
     */
    public URI getOpenAmApiBaseUrl() {
        return openAmApiBaseUrl;
    }

    /**
     * Returns the OpenAM realm URI
     * @return
     * The OpenAM realm URI
     */
    public URI getOpenAmApiRealmUrl() { return openAmApiRealmUrl; }

    /**
     * Returns the username used to authenticate against OpenAM
     * @return
     * The username used to authenticate against OpenAM
     */
    public String getUsername() { return username; }

    /**
     * Returns the password used to authenticate against OpenAM
     * @return
     * The username used to authenticate against OpenAM
     */
    public String getPassword() { return password; }

    private String validateProperty(String value, String variableName) {
        if (StringUtils.trimToNull(value) == null) {
            throw new IllegalStateException("Required configuration missing: " + variableName);
        }
        return value;
    }
}
