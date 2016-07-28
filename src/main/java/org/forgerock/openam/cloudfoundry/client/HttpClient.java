package org.forgerock.openam.cloudfoundry.client;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

public interface HttpClient {

    /**
     * Sends an HTTP request and returns a {@code Promise} representing the
     * pending HTTP response.
     *
     * @param request
     *            The HTTP request to send.
     * @return A promise representing the pending HTTP response.
     */
    Promise<Response, NeverThrowsException> send(Request request);

}
