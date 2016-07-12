package org.forgerock.openam.cloudfoundry;

import static org.forgerock.json.JsonValue.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.StringBody.exact;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.RootContext;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.Parameter;
import org.mockserver.socket.PortFactory;

public class ServiceBrokerTest {
    private static final String AUTHORIZATION_HEADER = "Basic dXNlcm5hbWU6cGFzc3dvcmQ="; // username:password
    private static ClientAndServer mockServerClient;

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
        assertThat(json(response.getEntity().getJson()), hasString("/services/0/name", is("openam-oauth2")));
    }

    @Test
    public void createBindingCreatesOAuth2ClientAndReturnsCredentials() throws Exception {
        mockServerClient.when(
                request()
                        .withMethod("POST")
                        .withPath("/json/realm/authenticate")
        ).respond(
                response()
                        .withBody("{\"tokenId\":\"ssoToken\"}")
                        .withStatusCode(200)
        );
        mockServerClient.when(
                request()
                        .withMethod("POST")
                        .withPath("/json/realm/agents")
        ).respond(
                response()
                        .withStatusCode(201)
        );

        Request request = new Request();
        request.getHeaders().add("Authorization", AUTHORIZATION_HEADER);
        request.setMethod("PUT");
        request.setUri("http://broker.example/v2/service_instances/instanceId/service_bindings/bindingId");
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
        assertThat(json(response.getEntity().getJson()), hasString("/credentials/username", is("instanceId-bindingId")));
        assertThat(json(response.getEntity().getJson()), hasString("/credentials/password", is("foo2")));

        mockServerClient.verify(
                request()
                        .withMethod("POST")
                        .withPath("/json/realm/authenticate")
                        .withQueryStringParameters(
                                new Parameter("authTokenType", "module"),
                                new Parameter("authIndexValue", "Application")
                        )
                        .withHeaders(
                                new Header("X-OpenAM-Username", "username"),
                                new Header("X-OpenAM-Password", "password")
                        ),
                request()
                        .withMethod("POST")
                        .withPath("/json/realm/agents")
                        .withQueryStringParameters(new Parameter("_action", "create"))
                        .withBody(exact("{\"username\":\"instanceId-bindingId\",\"userpassword\":\"foo\",\"AgentType\":\"OAuth2Client\",\"com.forgerock.openam.oauth2provider.name\":[\"[0]=instanceId-bindingId\"]}"))
                        .withHeader(new Header("iPlanetDirectoryPro", "ssoToken"))
        );
    }

    private ServiceBroker getServiceBroker() throws Exception {
        Configuration configuration = new Configuration("http://localhost:" + mockServerClient.getPort(), "/realm");
        return new ServiceBroker(configuration);
    }

    private Request createRequest(String method, String path) throws Exception {
        Request request = new Request();
        request.getHeaders().add("Authorization", AUTHORIZATION_HEADER);
        request.setMethod(method);
        request.setUri("http://broker.example/" + path);
        return request;
    }

    private Matcher<JsonValue> hasString(final String path, final Matcher<String> matcher) {
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
