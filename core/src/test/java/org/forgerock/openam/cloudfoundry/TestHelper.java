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

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.StringBody.exact;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;

public final class TestHelper {

    private TestHelper() {
    }

    public static void expectServerInfoCall(ClientAndServer mockServerClient, String cookieDomain) {
        mockServerClient.when(
                request()
                        .withMethod("GET")
                        .withPath("/json/serverinfo/*")
        ).respond(
                response()
                        .withBody("{\"cookieName\":\"" + cookieDomain + "\"}")
                        .withStatusCode(200));
    }

    public static void expectSuccessfulAuthentication(ClientAndServer mockServerClient, String ssoToken) {
        mockServerClient.when(
                request()
                        .withMethod("POST")
                        .withPath("/json/realm/authenticate")
        ).respond(
                response()
                        .withBody("{\"tokenId\":\"" + ssoToken + "\"}")
                        .withStatusCode(200));
    }

    public static void expectListClients(ClientAndServer mockServerClient, String... clientIds) {

        List<String> entries = new ArrayList<>();

        for (String id : clientIds) {
            entries.add("{\"username\": \"" + id + "\"}");
        }

        mockServerClient.when(
                request()
                        .withMethod("GET")
                        .withPath("/json/realm/agents")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withBody("{\"result\":[" + StringUtils.join(entries, ",") + "]}"));
    }

    public static void expectListClientsFailure(ClientAndServer mockServerClient) {

        mockServerClient.when(
                request()
                        .withMethod("GET")
                        .withPath("/json/realm/agents")
        ).respond(
                response()
                        .withStatusCode(500));
    }

    public static void expectClientCreationWithStatus(ClientAndServer mockServerClient, int statusCode) {
        mockServerClient.when(
                request()
                        .withMethod("POST")
                        .withPath("/json/realm/agents")
        ).respond(
                response()
                        .withStatusCode(statusCode));
    }

    public static void expectClientCreation(ClientAndServer mockServerClient) {
        expectClientCreationWithStatus(mockServerClient, 201);
    }

    public static void expectClientDeletion(ClientAndServer mockServerClient, String username, int statusCode) {
        mockServerClient.when(
                request()
                        .withMethod("DELETE")
                        .withPath("/json/realm/agents/" + username)
        ).respond(
                response()
                        .withStatusCode(statusCode));
    }

    public static HttpRequest verifyServerInfoCall() {
        return request()
                        .withMethod("GET")
                        .withPath("/json/serverinfo/*");
    }

    public static HttpRequest verifySuccessfulAuthentication() {
        return request()
                .withMethod("POST")
                .withPath("/json/realm/authenticate")
                .withQueryStringParameters(
                        new Parameter("authTokenType", "module"),
                        new Parameter("authIndexValue", "Application")
                )
                .withHeaders(
                        new Header("X-OpenAM-Username", "username"),
                        new Header("X-OpenAM-Password", "password"));
    }

    public static HttpRequest verifyListClients(String cookieName, String ssoToken) {
        return request()
                .withMethod("GET")
                .withPath("/json/realm/agents")
                .withHeader(new Header(cookieName, "ssoToken"))
                .withQueryStringParameter(new Parameter("_queryId", "*"));
    }

    public static HttpRequest verifyClientCreation(String username, String generatedPassword, String cookieName,
            String ssoToken) {
        return request()
                .withMethod("POST")
                .withPath("/json/realm/agents")
                .withQueryStringParameters(new Parameter("_action", "create"))
                .withBody(exact("{"
                        + "\"username\":\"" + username + "\","
                        + "\"userpassword\":\"" + generatedPassword + "\","
                        + "\"AgentType\":\"OAuth2Client\","
                        + "\"com.forgerock.openam.oauth2provider.name\":[\"[0]=" + username + "\"],"
                        + "\"com.forgerock.openam.oauth2provider.scopes\":[\"[0]=scope1\",\"[1]=scope2\"]"
                        + "}"))
                .withHeader(new Header(cookieName, ssoToken));
    }

    public static HttpRequest verifyClientDeletion(String username, String cookieName, String ssoToken) {
        return request()
                .withMethod("DELETE")
                .withPath("/json/realm/agents/" + username)
                .withHeader(new Header(cookieName, ssoToken));
    }

    public static Matcher<JsonValue> hasString(final String path, final Matcher<String> matcher) {
        return new TypeSafeDiagnosingMatcher<JsonValue>() {
            @Override
            public void describeTo(Description description) {
                matcher.describeTo(description);
                description.appendText(" at ").appendText(path);
            }

            @Override
            protected boolean matchesSafely(JsonValue item, Description mismatchDescription) {
                JsonValue value = item.get(new JsonPointer(path));
                if (value == null) {
                    mismatchDescription.appendText("was null");
                    mismatchDescription.appendText(" at ").appendText(path);
                    return false;
                } else if (!value.isString()) {
                    mismatchDescription.appendText("was ").appendText(value.getObject().getClass().getName());
                    mismatchDescription.appendText(" at ").appendText(path);
                    return false;
                } else if (!matcher.matches(value.asString())) {
                    matcher.describeMismatch(value.asString(), mismatchDescription);
                    mismatchDescription.appendText(" at ").appendText(path);
                    return false;
                }
                return true;
            }
        };
    }

}
