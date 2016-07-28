package org.forgerock.openam.cloudfoundry.client;

import org.forgerock.http.Client;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

public class HttpClientImpl implements HttpClient {

    private final Client client = new Client(new HttpClientHandler());

    public HttpClientImpl() throws HttpApplicationException { }

    @Override
    public Promise<Response, NeverThrowsException> send(Request request) {
        return client.send(request);
    }
}
