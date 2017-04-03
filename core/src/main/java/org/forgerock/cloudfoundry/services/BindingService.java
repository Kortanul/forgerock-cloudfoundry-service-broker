package org.forgerock.cloudfoundry.services;

import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Defines the operations needed to manage the binding of CloudFoundry services.
 */
public interface BindingService {

    /**
     * Register a new binding for this service.
     * @param instanceId the identifier of the service instance.
     * @param bindingId the identififer of the requested new binding.
     * @param bindResource the resource to bind.
     * @param parameters the parameters of the binding.
     * @return The response of the operation
     */
    Promise<Response, NeverThrowsException> bind(String instanceId, String bindingId, JsonValue bindResource,
            JsonValue parameters);

    /**
     * Unbind a binding for this service.
     * @param instanceId the identifier of the service instance.
     * @param bindingId the identififer of the requested new binding.
     * @return The response of the operation
     */
    Promise<Response, NeverThrowsException> unbind(String instanceId, String bindingId);
}

