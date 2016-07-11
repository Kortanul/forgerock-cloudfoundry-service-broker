package org.forgerock.openam.cloudfoundry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.StringBody.exact;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Parameter;
import org.mockserver.socket.PortFactory;

public class ServiceBrokerTest {
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
    public void test() throws Exception {
        mockServerClient.when(
                request()
                        .withMethod("POST")
                        .withPath("/json/realm/agents")
                        .withQueryStringParameters(new Parameter("_action", "create"))
                        .withBody(exact("{\"username\":\"instanceId-bindingId\"}"))
        ).respond(
                response()
                        .withStatusCode(201)
        );

        Request request = new Request();
        Configuration configuration = new Configuration("http://localhost:" + mockServerClient.getPort(), "/realm");
        ServiceBroker handler = new ServiceBroker(configuration);
        handler.handle(new RootContext(), request).then(new Function<Response, Void, NeverThrowsException>() {
            @Override
            public Void apply(Response response) throws NeverThrowsException {
                assertThat(response.getStatus().getCode(), is(200));
                return null;
            }
        });
    }
}
