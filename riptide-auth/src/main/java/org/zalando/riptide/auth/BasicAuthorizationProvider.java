package org.zalando.riptide.auth;

import java.nio.charset.*;
import java.util.*;

import static com.google.common.base.Preconditions.*;
import static java.nio.charset.StandardCharsets.*;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
 */
public final class BasicAuthorizationProvider implements AuthorizationProvider {

    private final String authorization;

    public BasicAuthorizationProvider(final String username, final String password) {
        checkArgument(!username.contains(":"), "Username must not contain a colon");
        final CharsetEncoder encoder = ISO_8859_1.newEncoder();
        checkArgument(encoder.canEncode(username), "Username must be encoded in ISO-8859-1");
        checkArgument(encoder.canEncode(password), "Password must be encoded in ISO-8859-1");
        final String credentials = username + ":" + password;
        final Base64.Encoder base64 = Base64.getEncoder();
        final byte[] bytes = credentials.getBytes(ISO_8859_1);
        this.authorization = "Basic " + base64.encodeToString(bytes);
    }

    @Override
    public String get() {
        return authorization;
    }

}
