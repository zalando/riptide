package org.zalando.riptide.auth;

import java.io.*;

public interface AuthorizationProvider {

    String get() throws IOException;

}
