/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.cloudfoundry.services.openam;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.cloudfoundry.OpenAMClient;
import org.forgerock.cloudfoundry.PasswordGenerator;
import org.forgerock.cloudfoundry.services.Service;
import org.forgerock.http.Handler;
import org.forgerock.json.JsonValue;

/**
 * Implementation of the openam-oauth2 service.
 */
public class OpenAMOAuth2Service implements Service {

    /** The identifier of the service. */
    public static final String SERVICE_ID = "3997be2d-e262-438e-8a31-8c90fa7156e5";

    /** The identifier of the plan. */
    public static final String PLAN_ID = "0140f6db-972a-466e-9e79-7845098a4ec7";

    private final Handler provisioningHandler;
    private final Handler bindingHandler;

    /**
     * Constructs a service that will handle the creation of OAuth2 clients in OpenAM.
     * @param openAMClient the client to use to call OpenAM
     * @param pwGen the password generator to use when creating the OAuth2 clients
     */
    public OpenAMOAuth2Service(OpenAMClient openAMClient, PasswordGenerator pwGen) {
        this.provisioningHandler = new ProvisioningHandler(openAMClient);
        this.bindingHandler = new BindingHandler(openAMClient, pwGen);
    }

    @Override
    public JsonValue getServiceMetadata() {
        return json(
                object(
                        field("id", SERVICE_ID),
                        field("name", "openam-oauth2"),
                        field("description", "Uses ForgeRock OpenAM to provide OAuth 2.0 authorization"),
                        field("tags", array("authentication", "oauth2")),
                        field("bindable", true),
                        field("metadata", object(
                                field("displayName", "ForgeRock OpenAM")
                        )),
                        field("plans", array(
                                object(
                                        field("id", PLAN_ID),
                                        field("name", "shared"),
                                        field("description", "Shared OpenAM server")
                                )
                        ))
                ));
    }

    @Override
    public Handler getProvisioningHandler() {
        return provisioningHandler;
    }

    @Override
    public Handler getBindingHandler() {
        return bindingHandler;
    }
}

