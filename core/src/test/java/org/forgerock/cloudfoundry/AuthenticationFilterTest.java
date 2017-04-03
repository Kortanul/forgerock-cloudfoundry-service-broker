package org.forgerock.cloudfoundry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.cloudfoundry.TestHelper.createBasicAuth;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class AuthenticationFilterTest {


    private AuthenticationFilter filter = new AuthenticationFilter("broker_user", "broker_password");

    @Mock
    private Handler next;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldAllowAccessWhenCorrectCredentials() throws Exception {
        Handler next = mock(Handler.class);
        Request request = new Request();
        request.getHeaders().put("Authorization", createBasicAuth("broker_user", "broker_password"));

        filter.filter(new RootContext(), request, next);

        verify(next).handle(any(Context.class), any(Request.class));
    }

    @Test
    public void shouldDenyAccessWith401WhenIncorrectCredentials() throws Exception {
        Request request = new Request();
        request.getHeaders().put("Authorization", createBasicAuth("broker_user", "bad_password"));

        Response response = filter.filter(new RootContext(), request, next).get();

        assertThat(response.getStatus()).isEqualTo(Status.UNAUTHORIZED);
        verifyZeroInteractions(next);
    }

    @Test
    public void shouldDenyAccessWith401WhenNoHeader() throws Exception {
        Request request = new Request();

        Response response = filter.filter(new RootContext(), request, next).get();

        assertThat(response.getStatus()).isEqualTo(Status.UNAUTHORIZED);
        verifyZeroInteractions(next);
    }
}
