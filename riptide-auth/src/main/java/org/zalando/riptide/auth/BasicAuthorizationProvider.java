package org.zalando.riptide.auth;

import java.nio.charset.CharsetEncoder;
import java.util.Base64;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
 */
public final class BasicAuthorizationProvider implements AuthorizationProvider {

    private final Base64.Encoder base64 = Base64.getEncoder();

    private final String username;
    private final String password;

    public BasicAuthorizationProvider(final String username, final String password) {
        checkArgument(!username.contains(":"), "Username must not contain a colon");
        final CharsetEncoder encoder = ISO_8859_1.newEncoder();
        checkArgument(encoder.canEncode(username), "Username must be encoded in ISO-8859-1");
        checkArgument(encoder.canEncode(password), "Password must be encoded in ISO-8859-1");
        this.username = username;
        this.password = password;
    }

    @Override
    public String get() {
        final String credentials = username + ":" + password;
        final byte[] bytes = credentials.getBytes(ISO_8859_1);
        return "Basic " + base64.encodeToString(bytes);
    }

}
