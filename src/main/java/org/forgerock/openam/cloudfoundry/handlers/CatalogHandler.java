package org.forgerock.openam.cloudfoundry.handlers;

import static org.forgerock.http.protocol.Status.METHOD_NOT_ALLOWED;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.cloudfoundry.Responses.newEmptyResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

public class CatalogHandler implements Handler {
    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        if (!"GET".equals(request.getMethod())) {
            return newResultPromise(newEmptyResponse(METHOD_NOT_ALLOWED));
        }
        JsonValue result = json(object(field("services", array(object(
                field("id", "3997be2d-e262-438e-8a31-8c90fa7156e5"),
                field("name", "openam-oauth2"),
                field("description", "Uses ForgeRock OpenAM to provide OAuth 2.0 authorization"),
                field("tags", array("authentication", "oauth2")),
                field("bindable", true),
                field("metadata", object(
                        field("displayName", "ForgeRock OpenAM")
                )),
                field("plans", array(
                        object(
                                field("id", "0140f6db-972a-466e-9e79-7845098a4ec7"),
                                field("name", "shared"),
                                field("description", "Shared OpenAM server")
                        )
                ))
        )))));
        return newResultPromise(newEmptyResponse(Status.OK).setEntity(result));
    }
}
