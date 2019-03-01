package org.zalando.riptide.auth;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class BasicAuthorizationProviderTest {

    @Test
    void shouldSupplyOAuthToken() throws IOException {
        final AuthorizationProvider unit = new BasicAuthorizationProvider("username", "password");

        assertEquals("Basic dXNlcm5hbWU6cGFzc3dvcmQ=", unit.get());
    }

    @Test
    void shouldFailIfUsernameContainsColon() {
        assertThrows(IllegalArgumentException.class, () ->
                new BasicAuthorizationProvider("user:name", ""));
    }

}
