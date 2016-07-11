package org.forgerock.openam.cloudfoundry;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;

public final class Responses {
    private Responses() {
    }

    public static Response newEmptyResponse(Status status) {
        return new Response(status).setEntity(json(object()));
    }
}
