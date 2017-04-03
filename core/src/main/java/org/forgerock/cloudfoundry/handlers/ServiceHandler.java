package org.forgerock.cloudfoundry.handlers;

import org.forgerock.cloudfoundry.services.Service;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

interface ServiceHandler {

    Promise<Response, NeverThrowsException> handle(Context context, Request request, Service service, JsonValue body);

}

