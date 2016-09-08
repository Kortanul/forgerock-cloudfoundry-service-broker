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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ConfigurationTest {

    private static final String EMPTY = "";
    private static final String MISSING = null;

    private Configuration config;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testOAuth2URIWithRootRealm() {
        config = new Configuration("http://host:port/openam/", "user", "password", "/", "broker_user",
                "broker_password", "scope1 scope2");

        assertThat(config.getOpenAmOAuth2Url().toString(), is("http://host:port/openam/oauth2/"));
    }

    @Test
    public void testOAuth2URIWithSubrealm() {
        config = new Configuration("http://host:port/openam/", "user", "password", "/subrealm", "broker_user",
                "broker_password", "scope1 scope2");

        assertThat(config.getOpenAmOAuth2Url().toString(), is("http://host:port/openam/oauth2/subrealm/"));
    }

    @Test
    public void testRootRealmURI() {
        config = new Configuration("http://host:port/openam/", "user", "password", "/", "broker_user",
                "broker_password", "scope1 scope2");

        assertThat(config.getOpenAmApiRealmUrl().toString(), is("http://host:port/openam/json/"));
    }

    @Test
    public void testMissingSuppliedRealm() {
        config = new Configuration("http://host:port/openam/", "user", "password", MISSING, "broker_user",
                "broker_password", "scope1 scope2");

        assertThat(config.getOpenAmApiRealmUrl().toString(), is("http://host:port/openam/json/"));
    }

    @Test
    public void testEmptySuppliedRealm() {
        config = new Configuration("http://host:port/openam/", "user", "password", EMPTY, "broker_user",
                "broker_password", "scope1 scope2");

        assertThat(config.getOpenAmApiRealmUrl().toString(), is("http://host:port/openam/json/"));
    }

    @Test
    public void testRealmURI() {
        config = new Configuration("http://host:port/openam/", "user", "password", "realm", "broker_user",
                "broker_password", "scope1 scope2");

        assertThat(config.getOpenAmApiRealmUrl().toString(), is("http://host:port/openam/json/realm/"));
    }

    @Test
    public void testUsername() {
        config = new Configuration("http://host:port/openam/", "user", "password", "realm", "broker_user",
                "broker_password", "scope1 scope2");

        assertThat(config.getOpenAmUsername(), is("user"));
    }

    @Test
    public void testPassword() {
        config = new Configuration("http://host:port/openam/", "user", "password", "realm", "broker_user",
                "broker_password", "scope1 scope2");

        assertThat(config.getOpenAmPassword(), is("password"));
    }

    @Test
    public void missingBaseURIThrowsException() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Required configuration missing: OPENAM_BASE_URI");

        config = new Configuration(MISSING, "user", "password", "realm", "broker_user", "broker_password",
                "scope1 scope2");
    }

    @Test
    public void emptyBaseURIThrowsException() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Required configuration missing: OPENAM_BASE_URI");

        config = new Configuration(EMPTY, "user", "password", "realm", "broker_user", "broker_password",
                "scope1 scope2");
    }

    @Test
    public void nonURIBaseURIThrowsException() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("OPENAM_BASE_URI is not a valid URI");

        config = new Configuration("not_a\\uri", "user", "password", "realm", "broker_user", "broker_password",
                "scope1 scope2");
    }

    @Test
    public void missingUsernameThrowsException() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Required configuration missing: OPENAM_USERNAME");

        config = new Configuration("http://host:port/openam/", MISSING, "password", "realm", "broker_user",
                "broker_password", "scope1 scope2");
    }

    @Test
    public void emptyUsernameThrowsException() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Required configuration missing: OPENAM_USERNAME");

        config = new Configuration("http://host:port/openam/", EMPTY, "password", "realm", "broker_user",
                "broker_password", "scope1 scope2");
    }

    @Test
    public void missingPasswordThrowsException() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Required configuration missing: OPENAM_PASSWORD");

        config = new Configuration("http://host:port/openam/", "user", MISSING, "realm", "broker_user",
                "broker_password", "scope1 scope2");
    }

    @Test
    public void emptyPasswordThrowsException() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Required configuration missing: OPENAM_PASSWORD");

        config = new Configuration("http://host:port/openam/", "user", EMPTY, "realm", "broker_user",
                "broker_password", "scope1 scope2");
    }

    @Test
    public void scopesShouldBeReadCorrectly() {
        config = new Configuration("http://host:port/openam/", "user", "password", "/", "broker_user",
                "broker_password", "scope1 scope2");

        assertThat(config.getScopes(), is((List<String>) Lists.newArrayList("scope1", "scope2")));
    }

}
