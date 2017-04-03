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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.guava.common.collect.Lists;

/**
 * An immutable container for the configuration of the broker.
 */
public final class Configuration {

    private final URI openAmOAuth2Url;
    private final URI openAmApiRealmUrl;
    private final String openAmUsername;
    private final String openAmPassword;
    private final String brokerUsername;
    private final String brokerPassword;
    private final URI openAmApiBaseUrl;
    private final List<String> scopes;

    /**
     * Constructs a new Configuration.
     * @param baseUri the base URI of OpenAM
     * @param openAmUsername the username to use to authenticate against OpenAM
     * @param openAmPassword the password to use to authenticate against OpenAM
     * @param realm the OpenAM realm to use
     * @param brokerUsername the username that clients to this broker are required to use.
     * @param brokerPassword the password that clients to this broker are required to use.
     * @param scopes A space delimited list of scopes that OAuth 2.0 clients will be created with.
     */
    public Configuration(String baseUri, String openAmUsername, String openAmPassword, String realm,
            String brokerUsername, String brokerPassword, String scopes) {
        URI openAmBaseUrl;
        try {
            openAmBaseUrl = new URI(validateProperty(baseUri, "OPENAM_BASE_URI") + "/");
        } catch (URISyntaxException e) {
            throw new IllegalStateException("OPENAM_BASE_URI is not a valid URI", e);
        }

        this.openAmUsername = validateProperty(openAmUsername, "OPENAM_USERNAME");
        this.openAmPassword = validateProperty(openAmPassword, "OPENAM_PASSWORD");
        this.brokerUsername = validateProperty(brokerUsername, "SECURITY_USER_NAME");
        this.brokerPassword = validateProperty(brokerPassword, "SECURITY_USER_PASSWORD");
        this.scopes = Lists.newArrayList(validateProperty(scopes, "OAUTH2_SCOPES").split(" "));

        realm = StringUtils.trimToEmpty(realm);
        openAmApiBaseUrl = openAmBaseUrl.resolve("json/");
        openAmApiRealmUrl = openAmBaseUrl.resolve("json/" + realm + "/");
        openAmOAuth2Url = openAmBaseUrl.resolve("oauth2/" + realm + "/");
    }

    /**
     * Returns the OpenAM base URI.
     * @return The OpenAM base URI
     */
    public URI getOpenAmApiBaseUrl() {
        return openAmApiBaseUrl;
    }

    /**
     * Returns the OpenAM realm URI.
     * @return The OpenAM realm URI
     */
    public URI getOpenAmApiRealmUrl() {
        return openAmApiRealmUrl;
    }

    /**
     * Returns the username used to authenticate against OpenAM.
     * @return The username used to authenticate against OpenAM
     */
    public String getOpenAmUsername() {
        return openAmUsername;
    }

    /**
     * Returns the password used to authenticate against OpenAM.
     * @return The password used to authenticate against OpenAM
     */
    public String getOpenAmPassword() {
        return openAmPassword;
    }

    private String validateProperty(String value, String variableName) {
        if (StringUtils.trimToNull(value) == null) {
            throw new IllegalStateException("Required configuration missing: " + variableName);
        }
        return value;
    }

    /**
     * Returns the OpenAM OAuth2 base URI.
     * @return The OAuth2 URI
     */
    public URI getOpenAmOAuth2Url() {
        return openAmOAuth2Url;
    }

    /**
     * Returns the username that clients to this broker are required to use.
     * @return a username
     */
    public String getBrokerUsername() {
        return brokerUsername;
    }

    /**
     * Returns the password that clients to this broker are required to use.
     * @return a password
     */
    public String getBrokerPassword() {
        return brokerPassword;
    }

    /**
     * Returns the set of supported scopes used when creating OAuth2 clients.
     * @return a set of scopes
     */
    public List<String> getScopes() {
        return scopes;
    }
}
