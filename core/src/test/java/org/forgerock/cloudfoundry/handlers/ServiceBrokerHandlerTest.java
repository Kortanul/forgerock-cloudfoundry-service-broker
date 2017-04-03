package org.forgerock.cloudfoundry.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.util.test.assertj.Conditions.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.forgerock.cloudfoundry.Configuration;
import org.forgerock.cloudfoundry.TestHelper;
import org.forgerock.cloudfoundry.services.Service;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.RootContext;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class ServiceBrokerHandlerTest {

    private String openigUri = "http://openig.example.com:8080/";

    @Test
    public void shouldRequireBasicAuthentication() throws Exception {
        Configuration configuration = new Configuration("http://host:port/openam/", "user",
                "password", "/", "broker_user", "broker_password",
                "scope1 scope2");
        ServiceBrokerHandler handler = new ServiceBrokerHandler(Collections.<String, Service>emptyMap(),
                configuration.getBrokerUsername(), configuration.getBrokerPassword());

        Response response = handler.handle(new RootContext(), new Request()).get();

        assertThat(response.getStatus()).isEqualTo(Status.UNAUTHORIZED);
    }

    @Test
    public void getCatalogRespondsWithCatalog() throws Exception {
        Configuration configuration = new Configuration("http://host:port/openam/", "user",
                "password", "/", "broker_user", "broker_password",
                "scope1 scope2");

        Service service = mock(Service.class);
        when(service.getServiceMetadata()).thenReturn(json(object(field("name", "openam-oauth2"))));
        Map<String, Service> services = Collections.singletonMap("foo", service);

        ServiceBrokerHandler handler = new ServiceBrokerHandler(services, configuration.getBrokerUsername(),
                configuration.getBrokerPassword());

        Request request = TestHelper.createRequest("GET", "v2/catalog");
        Response response = handler.handle(new RootContext(), request).get();
        assertThat(response.getStatus()).isEqualTo(Status.OK);
        JsonValue json = json(response.getEntity().getJson());
        assertThat(json).stringIs("/services/0/name", equalTo("openam-oauth2"));
    }

}
