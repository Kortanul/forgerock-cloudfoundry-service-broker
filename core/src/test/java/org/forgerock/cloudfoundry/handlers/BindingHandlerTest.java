package org.forgerock.cloudfoundry.handlers;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.routing.UriRouterContext.uriRouterContext;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.forgerock.cloudfoundry.services.BindingService;
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
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class BindingHandlerTest {

    private static final String INSTANCE_ID = "instanceId";
    private static final String SERVICE_ID = "serviceId";
    private static final String BINDING_ID = "bindingId";

    @Mock
    private Service service;

    @Mock
    private BindingService bindingService;

    @Captor
    private ArgumentCaptor<JsonValue> parameters;

    private BindingHandler bindingHandler;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        when(service.getBindingService()).thenReturn(bindingService);

        bindingHandler = new BindingHandler(singletonMap(SERVICE_ID, service));
    }

    @Test
    public void nonPutOrDeleteHttpVerbOnBindingReturnsNotSupported() throws Exception {
        Request request = new Request().setMethod("POST");

        Response response = bindingHandler.handle(context(), request).get();

        assertThat(response.getStatus()).isEqualTo(Status.METHOD_NOT_ALLOWED);
    }

    @Test
    public void shouldCallMethodBindOnTheRequestedService() throws Exception {
        Request request = new Request().setMethod("PUT");
        JsonValue body = json(object(field("service_id", SERVICE_ID)));
        request.getEntity().setJson(body);

        bindingHandler.handle(context(), request);

        verify(bindingService).bind(eq(INSTANCE_ID), eq(BINDING_ID), any(JsonValue.class), parameters.capture());
        assertThat(parameters.getValue().isNull()).isTrue();
    }

    @Test
    public void shouldCallMethodUnbindOnTheRequestedService() throws Exception {
        Request request = new Request().setMethod("DELETE").setUri("/does/not/matter");
        Form form = new Form();
        form.add("service_id", SERVICE_ID);
        form.appendRequestQuery(request);

        bindingHandler.handle(context(), request);

        verify(bindingService).unbind(eq(INSTANCE_ID), eq(BINDING_ID));
    }

    private Context context() {
        // The request has already been routed to here, the router has set the following template variables
        return uriRouterContext(new RootContext())
                    .templateVariable(INSTANCE_ID, INSTANCE_ID)
                    .templateVariable(BINDING_ID, BINDING_ID)
                    .build();
    }

    @Test
    public void shouldAnswerStatusBadRequestOnCreateWhenServiceNotFound() throws Exception {
        Request request = new Request().setMethod("PUT");
        JsonValue body = json(object(field("service_id", "unknown")));
        request.getEntity().setJson(body);

        Response response = bindingHandler.handle(context(), request).get();

        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST);
    }

    @Test
    public void shouldAnswerStatusBadRequestOnDeleteWhenServiceNotFound() throws Exception {
        Request request = new Request().setMethod("DELETE").setUri("/does/not/matter");
        Form form = new Form();
        form.add("service_id", "unknown");
        form.appendRequestQuery(request);

        Response response = bindingHandler.handle(context(), request).get();

        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST);
    }
}
