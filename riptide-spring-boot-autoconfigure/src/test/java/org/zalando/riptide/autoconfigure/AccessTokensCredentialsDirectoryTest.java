package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.riptide.auth.AuthorizationProvider;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE)
@ActiveProfiles(profiles = "credentials-directory", inheritProfiles = false)
@Component
final class AccessTokensCredentialsDirectoryTest {

    @Configuration
    @ImportAutoConfiguration({
            RiptideAutoConfiguration.class,
            JacksonAutoConfiguration.class,
    })
    public static class TestConfiguration {

    }

    @Autowired
    @Qualifier("example")
    private AuthorizationProvider provider;

    @Test
    void shouldUseCredentialsDirectory() throws IOException {
        assertEquals("Bearer eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.e30.", provider.get());
    }

}
