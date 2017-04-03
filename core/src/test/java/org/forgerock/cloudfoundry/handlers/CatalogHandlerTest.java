package org.forgerock.cloudfoundry.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.forgerock.cloudfoundry.services.Service;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.RootContext;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class CatalogHandlerTest {

    @Test
    public void nonGetHttpVerbOnCatalogReturnsNotSupported() throws Exception {
        Request request = new Request().setMethod("PUT");
        Response response = new CatalogHandler(Collections.<Service>emptyList()).handle(new RootContext(), request)
                .get();
        assertThat(response.getStatus()).isEqualTo(Status.METHOD_NOT_ALLOWED);
    }

    @Test
    public void shouldListAllTheServices() throws Exception {
        List<Service> services = new ArrayList<>();
        Service service1 = mock(Service.class);
        when(service1.getServiceMetadata()).thenReturn(json(object(field("foo_service1", "bar"))));
        services.add(service1);
        Service service2 = mock(Service.class);
        when(service2.getServiceMetadata()).thenReturn(json(object(field("foo_service2", "bar"))));
        services.add(service2);

        CatalogHandler catalogHandler = new CatalogHandler(services);

        Request request = new Request().setMethod("GET");
        Response response = catalogHandler.handle(new RootContext(), request).get();

        assertThat(response.getStatus()).isEqualTo(Status.OK);
        JsonValue expectedBody = json(object(
                field("services", array(
                        object(field("foo_service1", "bar")),
                        object(field("foo_service2", "bar"))))));
        assertThat(json(response.getEntity().getJson()).isEqualTo(expectedBody)).isTrue();
    }
}
