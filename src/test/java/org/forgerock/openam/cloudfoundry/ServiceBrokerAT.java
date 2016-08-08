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

import static org.forgerock.json.JsonValue.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.forgerock.openam.cloudfoundry.TestHelper.*;
import static org.mockito.Mockito.*;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.openam.cloudfoundry.client.HttpClient;
import org.forgerock.openam.cloudfoundry.client.HttpClientImpl;
import org.forgerock.services.context.RootContext;
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
        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = createRequest("GET", "v2/catalog");
        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));
        assertThat(json(response.getEntity().getJson()), hasString("/services/0/name", is("openam-oauth2")));

        mockServerClient.verify(
                verifyServerInfoCall()
        );
    }

    @Test
    public void nonGetHttpVerbOnCatalogReturnsNotSupported() throws Exception {
        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = createRequest("PUT", "v2/catalog");
        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(405));

        mockServerClient.verify(
                verifyServerInfoCall()
        );
    }

    @Test
    public void createBindingCreatesOAuth2ClientAndReturnsCredentials() throws Exception {
        String instanceId = "instanceId";
        String bindingId = "bindingId";
        String username = instanceId + "-" + bindingId;
        String generatedPassword = "foo2";

        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        when(mockPwGen.generatePassword()).thenReturn(generatedPassword);
        expectClientCreation(mockServerClient);

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
        assertThat(json(response.getEntity().getJson()), hasString("/credentials/username", is(username)));
        assertThat(json(response.getEntity().getJson()), hasString("/credentials/password", is(generatedPassword)));
        assertThat(json(response.getEntity().getJson()), hasString("/credentials/uri", is("http://localhost:" + mockServerClient.getPort() + "/oauth2/realm/")));

        mockServerClient.verify(
                verifyServerInfoCall(),
                verifySuccessfulAuthentication(),
                verifyClientCreation(username, generatedPassword, COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void createBindingWithMalformedBodyReturnsUnprocessableEntity() throws Exception {
        String instanceId = "instanceId";
        String bindingId = "bindingId";

        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = createRequest("PUT", "v2/service_instances/" + instanceId + "/service_bindings/" + bindingId);
        request.setEntity(json(object(
                field("service_id", "serviceId"),
                field("plan_id", "planId"),
                field("app_guid", "appGuid"),
                field("bind_resource", object())
        )));

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(422));

        mockServerClient.verify(
                verifyServerInfoCall()
        );
    }

    @Test
    public void createBindingWhichAlreadyExistsReturnsConflict() throws Exception {
        String instanceId = "instanceId";
        String bindingId = "bindingId";
        String username = instanceId + "-" + bindingId;
        String generatedPassword = "foo2";

        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        when(mockPwGen.generatePassword()).thenReturn(generatedPassword);
        expectClientCreationWithStatus(mockServerClient, 409);

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
                verifyServerInfoCall(),
                verifySuccessfulAuthentication(),
                verifyClientCreation(username, generatedPassword, COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void createBindingReturnsInternalServerErrorOnAMInternalServerError() throws Exception {
        String instanceId = "instanceId";
        String bindingId = "bindingId";
        String username = instanceId + "-" + bindingId;
        String generatedPassword = "foo2";

        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        when(mockPwGen.generatePassword()).thenReturn(generatedPassword);
        expectClientCreationWithStatus(mockServerClient, 500);

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
                verifyServerInfoCall(),
                verifySuccessfulAuthentication(),
                verifyClientCreation(username, generatedPassword, COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void deleteBindingDeletesOAuth2Client() throws Exception {
        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        expectClientDeletion(mockServerClient, "instanceId-bindingId", 200);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId/service_bindings/bindingId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));

        mockServerClient.verify(
                verifyServerInfoCall(),
                verifySuccessfulAuthentication(),
                verifyClientDeletion("instanceId-bindingId", COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void deleteBindingWhichDoesntExistReturnsGone() throws Exception {
        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        expectClientDeletion(mockServerClient, "instanceId-bindingId", 400);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId/service_bindings/bindingId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(410));

        mockServerClient.verify(
                verifyServerInfoCall(),
                verifySuccessfulAuthentication(),
                verifyClientDeletion("instanceId-bindingId", COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void nonPutOrDeleteHttpVerbOnBindingReturnsNotSupported() throws Exception {
        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = createRequest("POST", "v2/service_instances/instanceId/service_bindings/bindingId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(405));

        mockServerClient.verify(
                verifyServerInfoCall()
        );
    }

    @Test
    public void provisioningInstanceWithPut() throws Exception {
        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = createRequest("PUT", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));

        mockServerClient.verify(
                verifyServerInfoCall()
        );
    }

    @Test
    public void provisioningInstanceWithPatch() throws Exception {
        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = createRequest("PATCH", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));
    }

    @Test
    public void deprovisioningInstanceWithNoClients() throws Exception {
        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        expectListClients(mockServerClient);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));

        mockServerClient.verify(
                verifyServerInfoCall(),
                verifySuccessfulAuthentication(),
                verifyListClients(COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void deprovisioningInstanceRemovesSingleClient() throws Exception {
        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        expectListClients(mockServerClient, "instanceId-bindingId");
        expectClientDeletion(mockServerClient, "instanceId-bindingId", 201);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));

        mockServerClient.verify(
                verifyServerInfoCall(),
                verifySuccessfulAuthentication(),
                verifyListClients(COOKIE_DOMAIN, SSO_TOKEN),
                verifyClientDeletion("instanceId-bindingId", COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void deprovisioningInstanceWithSingleClientWhichHasBeenRemoved() throws Exception {
        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        expectListClients(mockServerClient, "instanceId-bindingId");
        expectClientDeletion(mockServerClient, "instanceId-bindingId", 400);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));

        mockServerClient.verify(
                verifyServerInfoCall(),
                verifySuccessfulAuthentication(),
                verifyListClients(COOKIE_DOMAIN, SSO_TOKEN),
                verifyClientDeletion("instanceId-bindingId", COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void deprovisioningInstanceRemovesOnlyClientsForThatInstance() throws Exception {
        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        expectListClients(mockServerClient, "instanceId-bindingId", "instanceId-bindingId2", "instanceId2-bindingId");
        expectClientDeletion(mockServerClient, "instanceId-bindingId", 201);
        expectClientDeletion(mockServerClient, "instanceId-bindingId2", 201);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));

        mockServerClient.verify(
                verifyServerInfoCall(),
                verifySuccessfulAuthentication(),
                verifyListClients(COOKIE_DOMAIN, SSO_TOKEN)
        );

        mockServerClient.verify(
                verifyClientDeletion("instanceId-bindingId", COOKIE_DOMAIN, SSO_TOKEN), VerificationTimes.once()
        );

        mockServerClient.verify(
                verifyClientDeletion("instanceId-bindingId2", COOKIE_DOMAIN, SSO_TOKEN), VerificationTimes.once()
        );
    }

    @Test
    public void nonPutPatchOrDeleteHttpVerbOnProvisioningReturnsNotSupported() throws Exception {
        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = createRequest("POST", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(405));

        mockServerClient.verify(
                verifyServerInfoCall()
        );
    }

    @Test
    public void deprovisionReturnsInternalServerErrorWhenRemovingClientFromAMReturnsInternalServerError() throws Exception{
        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        expectListClientsFailure(mockServerClient);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(500));

        mockServerClient.verify(
                verifyServerInfoCall(),
                verifySuccessfulAuthentication(),
                verifyListClients(COOKIE_DOMAIN, SSO_TOKEN)
        );
    }

    @Test
    public void deprovisionReturnsInternalServerErrorWhenGettingClientListFromAMReturnsInternalServerError() throws Exception{
        expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        expectListClients(mockServerClient, "instanceId-bindingId", "instanceId-bindingId2", "instanceId2-bindingId");
        expectClientDeletion(mockServerClient, "instanceId-bindingId", 201);
        expectClientDeletion(mockServerClient, "instanceId-bindingId2", 500);

        Request request = createRequest("DELETE", "v2/service_instances/instanceId");

        Response response = getServiceBroker().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(500));

        mockServerClient.verify(
                verifyServerInfoCall(),
                verifySuccessfulAuthentication(),
                verifyListClients(COOKIE_DOMAIN, SSO_TOKEN)
        );

        mockServerClient.verify(
                verifyClientDeletion("instanceId-bindingId", COOKIE_DOMAIN, SSO_TOKEN), VerificationTimes.once()
        );

        mockServerClient.verify(
                verifyClientDeletion("instanceId-bindingId2", COOKIE_DOMAIN, SSO_TOKEN), VerificationTimes.once()
        );
    }

    private ServiceBroker getServiceBroker() throws Exception {
        HttpClient client = new HttpClientImpl();
        Configuration configuration = new Configuration(
                "http://localhost:" + mockServerClient.getPort(),
                "username",
                "password",
                "/realm");

        return new ServiceBroker(client, configuration, mockPwGen);
    }

    private Request createRequest(String method, String path) throws Exception {
        Request request = new Request();
        request.setMethod(method);
        request.setUri("http://broker.example/" + path);
        return request;
    }

}
