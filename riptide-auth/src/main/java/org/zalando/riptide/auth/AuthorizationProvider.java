package org.zalando.riptide.auth;

import java.io.IOException;

public interface AuthorizationProvider {

    String get() throws IOException;

}
