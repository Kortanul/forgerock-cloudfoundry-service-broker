package org.forgerock.cloudfoundry.services;

import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Defines the operations needed to manage the provisioning of CloudFoundry services.
 */
public interface ProvisioningService {

    /**
     * Provision a new instance of the service.
     * @param instanceId the identifier of the requested new instance
     * @param parameters the parameters of the service instance
     * @return The response of the operation
     */
    Promise<Response, NeverThrowsException> provision(String instanceId, JsonValue parameters);

    /**
     * Update a instance of the service.
     * @param instanceId the identifier of the service instance to update
     * @param parameters the parameters of the service instance
     * @return The response of the operation
     */
    Promise<Response, NeverThrowsException> update(String instanceId, JsonValue parameters);

    /**
     * Deprovision a instance of the service.
     * @param instanceId the identifier of the service instance to deprovision
     * @return The response of the operation
     */
    Promise<Response, NeverThrowsException> deprovision(String instanceId);
}

