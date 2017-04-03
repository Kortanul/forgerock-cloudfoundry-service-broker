package org.forgerock.cloudfoundry.handlers;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.routing.UriRouterContext.uriRouterContext;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.forgerock.cloudfoundry.services.ProvisioningService;
import org.forgerock.cloudfoundry.services.Service;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class ProvisioningHandlerTest {

    private static final String INSTANCE_ID = "instanceId";
    private static final String SERVICE_ID = "serviceId";

    @Mock
    private Service service;

    @Mock
    private ProvisioningService provisioningService;

    @Captor
    private ArgumentCaptor<JsonValue> parameters;

    private ProvisioningHandler provisioningHandler;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        when(service.getProvisioningService()).thenReturn(provisioningService);

        provisioningHandler = new ProvisioningHandler(singletonMap(SERVICE_ID, service));
    }

    @Test
    public void nonPutOrPatchOrDeleteHttpVerbOnBindingReturnsNotSupported() throws Exception {
        Request request = new Request().setMethod("POST");
        request.getEntity().setJson(json(object()));

        Response response = provisioningHandler.handle(context(), request).get();

        assertThat(response.getStatus()).isEqualTo(Status.METHOD_NOT_ALLOWED);
    }

    @Test
    public void shouldCallMethodProvisionOnTheRequestedService() throws Exception {
        Request request = new Request().setMethod("PUT");
        JsonValue body = json(object(field("service_id", SERVICE_ID)));
        request.getEntity().setJson(body);

        provisioningHandler.handle(context(), request);

        verify(provisioningService).provision(eq(INSTANCE_ID), parameters.capture());
        assertThat(parameters.getValue().isNull()).isTrue();
    }

    @Test
    public void shouldCallMethodUpdateOnTheRequestedService() throws Exception {
        Request request = new Request().setMethod("PATCH");
        JsonValue body = json(object(field("service_id", SERVICE_ID)));
        request.getEntity().setJson(body);

        provisioningHandler.handle(context(), request);

        verify(provisioningService).update(eq(INSTANCE_ID), parameters.capture());
        assertThat(parameters.getValue().isNull()).isTrue();
    }

    @Test
    public void shouldCallMethodDeprovisionOnTheRequestedService() throws Exception {
        Request request = new Request().setMethod("DELETE").setUri("/does/not/matter");
        Form form = new Form();
        form.add("service_id", SERVICE_ID);
        form.appendRequestQuery(request);

        provisioningHandler.handle(context(), request);

        verify(provisioningService).deprovision(eq(INSTANCE_ID));
    }

    @DataProvider(name = "httpMethods")
    public static Object[][] httpMethods() {
        //@Checkstyle:off
        return new Object[][]{
                { "PUT" },
                { "PATCH" }
        };
        //@Checkstyle:on
    }

    @Test(dataProvider = "httpMethods")
    public void shouldAnswerStatusBadRequestOnCreateWhenServiceNotFound(String method) throws Exception {
        Request request = new Request().setMethod(method);
        JsonValue body = json(object(field("service_id", "unknown")));
        request.getEntity().setJson(body);

        Response response = provisioningHandler.handle(context(), request).get();

        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST);
    }

    @Test
    public void shouldAnswerStatusBadRequestOnDeleteWhenServiceNotFound() throws Exception {
        Request request = new Request().setMethod("DELETE").setUri("/does/not/matter");
        Form form = new Form();
        form.add("service_id", "unknown");
        form.appendRequestQuery(request);

        Response response = provisioningHandler.handle(context(), request).get();

        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST);
    }

    private Context context() {
        // The request has already been routed to here, the router has set the following template variables
        return uriRouterContext(new RootContext())
                .templateVariable(INSTANCE_ID, INSTANCE_ID)
                .build();
    }
}
