package org.forgerock.openam.cloudfoundry;

import java.net.URI;
import java.net.URISyntaxException;

public class Configuration {
    private final URI openAmApiBaseUrl;

    public Configuration() throws URISyntaxException {
        String baseUri = System.getenv("OPENAM_BASE_URL");
        if (baseUri == null) {
            throw new IllegalStateException("Missing required environment variable OPENAM_BASE_URL");
        }
        URI apiBaseUri = new URI(baseUri + "/").resolve("json/");
        String realm = System.getenv("OPENAM_REALM");
        if (realm != null) {
            apiBaseUri = apiBaseUri.resolve(realm + "/");
        }
        openAmApiBaseUrl = apiBaseUri;
    }

    public URI getOpenAmApiBaseUrl() {
        return openAmApiBaseUrl;
    }
}
