package org.forgerock.openam.cloudfoundry;

import java.net.URISyntaxException;

public class ConfigurationEnvironmentReader {
    public Configuration read() throws URISyntaxException {
        return new Configuration(System.getenv("OPENAM_BASE_URI"), System.getenv("OPENAM_REALM"));
    }
}
