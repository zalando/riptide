package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.jackson.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.test.context.*;
import org.zalando.riptide.auth.*;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;

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
