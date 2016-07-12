package org.forgerock.openam.cloudfoundry;

import java.net.URI;
import java.net.URISyntaxException;

public class Configuration {
    private final URI openAmApiBaseUrl;

    public Configuration(String baseUri, String realm) throws URISyntaxException {
        if (baseUri == null) {
            throw new IllegalStateException("Missing required environment variable OPENAM_BASE_URL");
        }
        URI apiBaseUri = new URI(baseUri + "/").resolve("json/");
        if (realm != null) {
            apiBaseUri = apiBaseUri.resolve(realm.replaceFirst("^/", "") + "/");
        }
        openAmApiBaseUrl = apiBaseUri;
    }

    public URI getOpenAmApiBaseUrl() {
        return openAmApiBaseUrl;
    }
}
