package org.zalando.riptide.auth;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PlatformCredentialsAuthorizationProviderTest {

    @Test
    void shouldSupplyOAuthToken() throws IOException {
        final AuthorizationProvider unit = new PlatformCredentialsAuthorizationProvider(
                Paths.get("src/test/resources/meta/credentials"), "example");

        assertEquals("Bearer eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.e30.", unit.get());
    }

    @Test
    void shouldFailOnMissingFile() {
        final AuthorizationProvider unit = new PlatformCredentialsAuthorizationProvider("missing");
        assertThrows(NoSuchFileException.class, unit::get);
    }

    @Test
    void shouldFailOnEmptyFile() {
        final AuthorizationProvider unit = new PlatformCredentialsAuthorizationProvider(
                Paths.get("src/test/resources/meta/credentials"), "empty");
        assertThrows(IndexOutOfBoundsException.class, unit::get);
    }

}
