package org.forgerock.openam.cloudfoundry;

import org.forgerock.services.context.AbstractContext;
import org.forgerock.services.context.Context;

public class BasicAuthorizationContext extends AbstractContext {
    private final String username;
    private final String password;

    public BasicAuthorizationContext(Context parent, String username, String password) {
        super(parent, "basicAuthorization");
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
