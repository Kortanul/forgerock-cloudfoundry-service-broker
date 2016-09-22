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

import static org.forgerock.json.JsonValue.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.encode.Base64;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;
import org.mockserver.verify.VerificationTimes;

public class ServiceBrokerAT {

    private static final String COOKIE_DOMAIN = "CookieDomain";
    private static final String SSO_TOKEN = "ssoToken";

    private static ClientAndServer mockServerClient;

    private PasswordGenerator mockPwGen = mock(PasswordGenerator.class);

    @BeforeClass
    public static void startServer() {
        mockServerClient = ClientAndServer.startClientAndServer(PortFactory.findFreePort());
    }

    @AfterClass
    public static void stopServer() {
        mockServerClient.stop();
    }

    @Before
    public void reset() {
        mockServerClient.reset();
    }

    @Test
    public void getCatalogRespondsWithCatalog() throws Exception {
        Request request = createRequest("GET", "v2/catalog");
        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));
        assertThat(json(response.getEntity().getJson()), TestHelper.hasString("/services/0/name", is("openam-oauth2")));
    }

    @Test
    public void nonGetHttpVerbOnCatalogReturnsNotSupported() throws Exception {
        Request request = createRequest("PUT", "v2/catalog");
        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(405));
    }

    @Test
    public void createBindingCreatesOAuth2ClientAndReturnsCredentials() throws Exception {
        String instanceId = "instanceId";
        String bindingId = "bindingId";
        String username = instanceId + "-" + bindingId;
        String generatedPassword = "foo2";

        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        TestHelper.expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        when(mockPwGen.generatePassword()).thenReturn(generatedPassword);
        TestHelper.expectClientCreation(mockServerClient);

        Request request = createRequest("PUT", "v2/service_instances/" + instanceId + "/service_bindings/" + bindingId);
        request.setEntity(json(object(
                field("service_id", "serviceId"),
                field("plan_id", "planId"),
                field("app_guid", "appGuid"),
                field("bind_resource", object(
                        field("app_guid", "appGuid")
                ))
        )));

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(201));
        assertThat(json(response.getEntity().getJson()), TestHelper.hasString("/credentials/username", is(username)));
        assertThat(json(response.getEntity().getJson()), TestHelper.hasString("/credentials/password",
                is(generatedPassword)));
        assertThat(json(response.getEntity().getJson()), TestHelper.hasString("/credentials/uri", is("http://localhost:"
                + mockServerClient.getPort() + "/oauth2/realm/")));

        mockServerClient.verify(
                TestHelper.verifySuccessfulAuthentication(),
                TestHelper.verifyServerInfoCall(),
                TestHelper.verifyClientCreation(username, generatedPassword, COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void createBindingWithMalformedBodyReturnsUnprocessableEntity() throws Exception {
        String instanceId = "instanceId";
        String bindingId = "bindingId";

        Request request = createRequest("PUT", "v2/service_instances/" + instanceId + "/service_bindings/" + bindingId);
        request.setEntity(json(object(
                field("service_id", "serviceId"),
                field("plan_id", "planId"),
                field("app_guid", "appGuid"),
                field("bind_resource", object())
        )));

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(422));
    }

    @Test
    public void createBindingWhichAlreadyExistsReturnsConflict() throws Exception {
        String instanceId = "instanceId";
        String bindingId = "bindingId";
        String username = instanceId + "-" + bindingId;
        String generatedPassword = "foo2";

        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        TestHelper.expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        when(mockPwGen.generatePassword()).thenReturn(generatedPassword);
        TestHelper.expectClientCreationWithStatus(mockServerClient, 409);

        Request request = createRequest("PUT", "v2/service_instances/" + instanceId + "/service_bindings/" + bindingId);
        request.setEntity(json(object(
                field("service_id", "serviceId"),
                field("plan_id", "planId"),
                field("app_guid", "appGuid"),
                field("bind_resource", object(
                        field("app_guid", "appGuid")
                ))
        )));

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(409));

        mockServerClient.verify(
                TestHelper.verifySuccessfulAuthentication(),
                TestHelper.verifyServerInfoCall(),
                TestHelper.verifyClientCreation(username, generatedPassword, COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void createBindingReturnsInternalServerErrorOnAMInternalServerError() throws Exception {
        String instanceId = "instanceId";
        String bindingId = "bindingId";
        String username = instanceId + "-" + bindingId;
        String generatedPassword = "foo2";

        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        TestHelper.expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        when(mockPwGen.generatePassword()).thenReturn(generatedPassword);
        TestHelper.expectClientCreationWithStatus(mockServerClient, 500);

        Request request = createRequest("PUT", "v2/service_instances/" + instanceId + "/service_bindings/" + bindingId);
        request.setEntity(json(object(
                field("service_id", "serviceId"),
                field("plan_id", "planId"),
                field("app_guid", "appGuid"),
                field("bind_resource", object(
                        field("app_guid", "appGuid")
                ))
        )));

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(500));

        mockServerClient.verify(
                TestHelper.verifySuccessfulAuthentication(),
                TestHelper.verifyServerInfoCall(),
                TestHelper.verifyClientCreation(username, generatedPassword, COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void deleteBindingDeletesOAuth2Client() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        TestHelper.expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        TestHelper.expectClientDeletion(mockServerClient, "instanceId-bindingId", 200);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId/service_bindings/bindingId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));

        mockServerClient.verify(
                TestHelper.verifySuccessfulAuthentication(),
                TestHelper.verifyServerInfoCall(),
                TestHelper.verifyClientDeletion("instanceId-bindingId", COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void deleteBindingWhichDoesntExistReturnsGone() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        TestHelper.expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        TestHelper.expectClientDeletion(mockServerClient, "instanceId-bindingId", 400);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId/service_bindings/bindingId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(410));

        mockServerClient.verify(
                TestHelper.verifySuccessfulAuthentication(),
                TestHelper.verifyServerInfoCall(),
                TestHelper.verifyClientDeletion("instanceId-bindingId", COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void nonPutOrDeleteHttpVerbOnBindingReturnsNotSupported() throws Exception {
        Request request = createRequest("POST", "v2/service_instances/instanceId/service_bindings/bindingId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(405));
    }

    @Test
    public void provisioningInstanceWithPut() throws Exception {
        Request request = createRequest("PUT", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));
    }

    @Test
    public void provisioningInstanceWithPatch() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = createRequest("PATCH", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));
    }

    @Test
    public void deprovisioningInstanceWithNoClients() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        TestHelper.expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        TestHelper.expectListClients(mockServerClient);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));

        mockServerClient.verify(
                TestHelper.verifySuccessfulAuthentication(),
                TestHelper.verifyServerInfoCall(),
                TestHelper.verifyListClients(COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void deprovisioningInstanceRemovesSingleClient() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        TestHelper.expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        TestHelper.expectListClients(mockServerClient, "instanceId-bindingId");
        TestHelper.expectClientDeletion(mockServerClient, "instanceId-bindingId", 201);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));

        mockServerClient.verify(
                TestHelper.verifySuccessfulAuthentication(),
                TestHelper.verifyServerInfoCall(),
                TestHelper.verifyListClients(COOKIE_DOMAIN, SSO_TOKEN),
                TestHelper.verifyClientDeletion("instanceId-bindingId", COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void deprovisioningInstanceWithSingleClientWhichHasBeenRemoved() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        TestHelper.expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        TestHelper.expectListClients(mockServerClient, "instanceId-bindingId");
        TestHelper.expectClientDeletion(mockServerClient, "instanceId-bindingId", 400);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));

        mockServerClient.verify(
                TestHelper.verifySuccessfulAuthentication(),
                TestHelper.verifyServerInfoCall(),
                TestHelper.verifyListClients(COOKIE_DOMAIN, SSO_TOKEN),
                TestHelper.verifyClientDeletion("instanceId-bindingId", COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void deprovisioningInstanceRemovesOnlyClientsForThatInstance() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        TestHelper.expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        TestHelper.expectListClients(mockServerClient, "instanceId-bindingId", "instanceId-bindingId2",
                "instanceId2-bindingId");
        TestHelper.expectClientDeletion(mockServerClient, "instanceId-bindingId", 201);
        TestHelper.expectClientDeletion(mockServerClient, "instanceId-bindingId2", 201);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));

        mockServerClient.verify(
                TestHelper.verifySuccessfulAuthentication(),
                TestHelper.verifyServerInfoCall(),
                TestHelper.verifyListClients(COOKIE_DOMAIN, SSO_TOKEN)
        );

        mockServerClient.verify(
                TestHelper.verifyClientDeletion("instanceId-bindingId", COOKIE_DOMAIN, SSO_TOKEN),
                VerificationTimes.once()
        );

        mockServerClient.verify(
                TestHelper.verifyClientDeletion("instanceId-bindingId2", COOKIE_DOMAIN, SSO_TOKEN),
                VerificationTimes.once()
        );
    }

    @Test
    public void nonPutPatchOrDeleteHttpVerbOnProvisioningReturnsNotSupported() throws Exception {
        Request request = createRequest("POST", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(405));
    }

    @Test
    public void deprovisionReturnsInternalServerErrorWhenRemovingClientFromAMReturnsInternalServerError()
            throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        TestHelper.expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        TestHelper.expectListClientsFailure(mockServerClient);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(500));

        mockServerClient.verify(
                TestHelper.verifySuccessfulAuthentication(),
                TestHelper.verifyServerInfoCall(),
                TestHelper.verifyListClients(COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void deprovisionReturnsInternalServerErrorWhenGettingClientListFromAMReturnsInternalServerError()
            throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        TestHelper.expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        TestHelper.expectListClients(mockServerClient, "instanceId-bindingId", "instanceId-bindingId2",
                "instanceId2-bindingId");
        TestHelper.expectClientDeletion(mockServerClient, "instanceId-bindingId", 201);
        TestHelper.expectClientDeletion(mockServerClient, "instanceId-bindingId2", 500);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(500));

        mockServerClient.verify(
                TestHelper.verifySuccessfulAuthentication(),
                TestHelper.verifyServerInfoCall(),
                TestHelper.verifyListClients(COOKIE_DOMAIN, SSO_TOKEN)
        );

        mockServerClient.verify(
                TestHelper.verifyClientDeletion("instanceId-bindingId", COOKIE_DOMAIN, SSO_TOKEN),
                VerificationTimes.once()
        );

        mockServerClient.verify(
                TestHelper.verifyClientDeletion("instanceId-bindingId2", COOKIE_DOMAIN, SSO_TOKEN),
                VerificationTimes.once()
        );
    }

    @Test
    public void getCatalogRequiresAuthorization() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = createRequest("GET", "v2/catalog");
        request.getHeaders().remove("Authorization");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(401));
    }

    @Test
    public void createBindingRequiresAuthorization() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = createRequest("PUT", "v2/service_instances/instanceId/service_bindings/bindingId");
        request.getHeaders().remove("Authorization");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(401));
    }

    @Test
    public void deleteBindingRequiresAuthorization() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId/service_bindings/bindingId");
        request.getHeaders().remove("Authorization");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(401));
    }

    @Test
    public void provisioningInstanceRequiresAuthorization() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = createRequest("PUT", "v2/service_instances/instanceId");
        request.getHeaders().remove("Authorization");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(401));
    }

    @Test
    public void deprovisioningInstanceRequiresAuthorization() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId");
        request.getHeaders().remove("Authorization");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(401));
    }

    @Test
    public void invalidCredentialsReturns401() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = createRequest("GET", "v2/catalog");
        request.getHeaders().put("Authorization", createBasicAuth("broker_user", "wrong_password"));

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(401));
    }

    @Test
    public void invalidAuthTypeReturns401() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = createRequest("GET", "v2/catalog");
        request.getHeaders().put("Authorization", "Bearer 1234");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(401));
    }

    private ServiceBroker getServiceBroker() throws Exception {
        Configuration configuration = new Configuration(
                "http://localhost:" + mockServerClient.getPort(),
                "username",
                "password",
                "/realm",
                "broker_user",
                "broker_password",
                "scope1 scope2");

        return new ServiceBroker(configuration, mockPwGen);
    }

    private Request createRequest(String method, String path) throws Exception {
        Request request = new Request();
        request.setMethod(method);
        request.setUri("http://broker.example/" + path);
        request.getHeaders().put("Authorization", createBasicAuth("broker_user", "broker_password"));
        return request;
    }

    private String createBasicAuth(String username, String password) {
        return "Basic " + Base64.encode((username + ":" + password).getBytes(StandardCharsets.ISO_8859_1));
    }
}
