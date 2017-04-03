package org.forgerock.cloudfoundry.services.openam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.util.test.assertj.Conditions.equalTo;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;

import org.forgerock.cloudfoundry.OpenAMClient;
import org.forgerock.cloudfoundry.PasswordGenerator;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promises;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class OpenAMOAuth2BindingServiceTest {

    @Mock
    private PasswordGenerator mockPwGen;

    @Mock
    private OpenAMClient openAMClient;

    private final String instanceId = "instanceId";
    private final String bindingId = "bindingId";
    private final String username = instanceId + "-" + bindingId;
    private final String generatedPassword = "foo2";

    private OpenAMOAuth2BindingService service;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        when(mockPwGen.generatePassword()).thenReturn(generatedPassword);
        service = new OpenAMOAuth2BindingService(openAMClient, mockPwGen);
    }

    @Test
    public void createBindingCreatesOAuth2ClientAndReturnsCredentials() throws Exception {
        when(openAMClient.createClient(anyString(), anyString()))
                .thenReturn(Promises.<Response, NeverThrowsException>newResultPromise(new Response(Status.OK)
                        .setEntity(json(object()))));
        when(openAMClient.getOAuth2Endpoint())
                .thenReturn(URI.create("http://openam.example.com/oauth2/realm"));

        JsonValue bindResource = json(object(field("app_guid", "appGuid")));
        JsonValue parameters = json(object());

        Response response = service.bind(instanceId, bindingId, bindResource, parameters).get();
        assertThat(response.getStatus()).isEqualTo(Status.CREATED);
        JsonValue credentials = json(response.getEntity().getJson()).get("credentials");
        assertThat(credentials).stringIs("/username", equalTo(username));
        assertThat(credentials).stringIs("/password", equalTo(generatedPassword));
        assertThat(credentials).stringIs("/uri", equalTo("http://openam.example.com/oauth2/realm"));
    }

    @Test
    public void createBindingWithMalformedBodyReturnsUnprocessableEntity() throws Exception {
        JsonValue bindResource = json(object());
        JsonValue parameters = json(object());

        Response response = service.bind(instanceId, bindingId, bindResource, parameters).get();
        assertThat(response.getStatus().getCode()).isEqualTo(422);
    }

    @Test
    public void createBindingWhichAlreadyExistsReturnsConflict() throws Exception {
        when(openAMClient.createClient(eq(username), eq(generatedPassword)))
                .thenReturn(Promises.<Response, NeverThrowsException>newResultPromise(new Response(Status.CONFLICT)));

        JsonValue bindResource = json(object(field("app_guid", "appGuid")));
        JsonValue parameters = json(object());

        Response response = service.bind(instanceId, bindingId, bindResource, parameters).get();

        verify(openAMClient).createClient(eq(username), eq(generatedPassword));
        assertThat(response.getStatus()).isEqualTo(Status.CONFLICT);
    }

    @Test
    public void createBindingReturnsInternalServerErrorOnAMInternalServerError() throws Exception {
        when(openAMClient.createClient(eq(username), eq(generatedPassword)))
                .thenReturn(Promises.<Response, NeverThrowsException>newResultPromise(
                        new Response(Status.INTERNAL_SERVER_ERROR)));

        JsonValue bindResource = json(object(field("app_guid", "appGuid")));
        JsonValue parameters = json(object());

        Response response = service.bind(instanceId, bindingId, bindResource, parameters).get();

        verify(openAMClient).createClient(eq(username), eq(generatedPassword));
        assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void deleteBindingDeletesOAuth2Client() throws Exception {
        when(openAMClient.deleteClient(eq(username)))
                .thenReturn(Promises.<Response, NeverThrowsException>newResultPromise(new Response(Status.OK)));

        Response response = service.unbind(instanceId, bindingId).get();

        verify(openAMClient).deleteClient(username);
        assertThat(response.getStatus()).isEqualTo(Status.OK);
    }

    @Test
    public void deleteBindingWhichDoesNotExistReturnsGone() throws Exception {
        when(openAMClient.deleteClient(eq(username)))
                .thenReturn(Promises.<Response, NeverThrowsException>newResultPromise(
                        new Response(Status.BAD_REQUEST)));
        Response response = service.unbind(instanceId, bindingId).get();

        verify(openAMClient).deleteClient(username);
        assertThat(response.getStatus()).isEqualTo(Status.GONE);
    }
}
