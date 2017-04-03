package org.forgerock.cloudfoundry.services.openam;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import org.forgerock.cloudfoundry.Configuration;
import org.forgerock.cloudfoundry.OpenAMClient;
import org.forgerock.cloudfoundry.PasswordGenerator;
import org.forgerock.cloudfoundry.ServiceBroker;
import org.forgerock.cloudfoundry.TestHelper;
import org.forgerock.cloudfoundry.handlers.ServiceBrokerHandler;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.services.context.RootContext;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;
import org.mockserver.verify.VerificationTimes;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class OpenAMOAuth2ProvisioningServiceTest {

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

    @BeforeMethod
    public void reset() {
        mockServerClient.reset();
    }

    @Test
    public void provisioningInstanceWithPut() throws Exception {
        OpenAMOAuth2ProvisioningService service = new OpenAMOAuth2ProvisioningService(mock(OpenAMClient.class));


        Request request = TestHelper.createRequest("PUT", "v2/service_instances/instanceId");
        request.setEntity(json(object(
                field("service_id", OpenAMOAuth2Service.SERVICE_ID),
                field("plan_id", "planId")
        )));

        Response response = getServiceBrokerHandler().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));
    }

    @Test
    public void provisioningInstanceWithPatch() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = TestHelper.createRequest("PATCH", "v2/service_instances/instanceId");
        request.setEntity(json(object(
                field("service_id", OpenAMOAuth2Service.SERVICE_ID),
                field("plan_id", "planId")
        )));

        Response response = getServiceBrokerHandler().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(200));
    }

    @Test
    public void deprovisioningInstanceWithNoClients() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        TestHelper.expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        TestHelper.expectListClients(mockServerClient);

        Request request = TestHelper.createRequest("DELETE", "v2/service_instances/instanceId");
        Form form = new Form();
        form.add("service_id", OpenAMOAuth2Service.SERVICE_ID);
        form.add("plan_id", "planId");
        form.appendRequestQuery(request);

        Response response = getServiceBrokerHandler().handle(new RootContext(), request).get();
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

        Request request = TestHelper.createRequest("DELETE", "v2/service_instances/instanceId");
        Form form = new Form();
        form.add("service_id", OpenAMOAuth2Service.SERVICE_ID);
        form.add("plan_id", "planId");
        form.appendRequestQuery(request);

        Response response = getServiceBrokerHandler().handle(new RootContext(), request).get();
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

        Request request = TestHelper.createRequest("DELETE", "v2/service_instances/instanceId");
        Form form = new Form();
        form.add("service_id", OpenAMOAuth2Service.SERVICE_ID);
        form.add("plan_id", "planId");
        form.appendRequestQuery(request);

        Response response = getServiceBrokerHandler().handle(new RootContext(), request).get();
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

        Request request = TestHelper.createRequest("DELETE", "v2/service_instances/instanceId");
        Form form = new Form();
        form.add("service_id", OpenAMOAuth2Service.SERVICE_ID);
        form.add("plan_id", "planId");
        form.appendRequestQuery(request);

        Response response = getServiceBrokerHandler().handle(new RootContext(), request).get();
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
        Request request = TestHelper.createRequest("POST", "v2/service_instances/instanceId");

        Response response = getServiceBrokerHandler().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(405));
    }

    @Test
    public void deprovisionReturnsInternalServerErrorWhenRemovingClientFromAMReturnsInternalServerError()
            throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);
        TestHelper.expectSuccessfulAuthentication(mockServerClient, SSO_TOKEN);
        TestHelper.expectListClientsFailure(mockServerClient);

        Request request = TestHelper.createRequest("DELETE", "v2/service_instances/instanceId");
        Form form = new Form();
        form.add("service_id", OpenAMOAuth2Service.SERVICE_ID);
        form.add("plan_id", "planId");
        form.appendRequestQuery(request);

        Response response = getServiceBrokerHandler().handle(new RootContext(), request).get();
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

        Request request = TestHelper.createRequest("DELETE", "v2/service_instances/instanceId");
        Form form = new Form();
        form.add("service_id", OpenAMOAuth2Service.SERVICE_ID);
        form.add("plan_id", "planId");
        form.appendRequestQuery(request);

        Response response = getServiceBrokerHandler().handle(new RootContext(), request).get();
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
    public void provisioningInstanceRequiresAuthorization() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = TestHelper.createRequest("PUT", "v2/service_instances/instanceId");
        request.getHeaders().remove("Authorization");

        Response response = getServiceBrokerHandler().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(401));
    }

    @Test
    public void deprovisioningInstanceRequiresAuthorization() throws Exception {
        TestHelper.expectServerInfoCall(mockServerClient, COOKIE_DOMAIN);

        Request request = TestHelper.createRequest("DELETE", "v2/service_instances/instanceId");
        request.getHeaders().remove("Authorization");

        Response response = getServiceBrokerHandler().handle(new RootContext(), request).get();
        assertThat(response.getStatus().getCode(), is(401));
    }

    private ServiceBrokerHandler getServiceBrokerHandler() throws Exception {
        Configuration configuration = new Configuration(
                "http://localhost:" + mockServerClient.getPort(),
                "username",
                "password",
                "/realm",
                "broker_user",
                "broker_password",
                "scope1 scope2");

        ServiceBroker broker = new ServiceBroker(configuration, mockPwGen);
        return new ServiceBrokerHandler(broker.getServices(), configuration.getBrokerUsername(),
                configuration.getBrokerPassword());
    }

}
