package org.zalando.riptide.autoconfigure;

import com.google.gag.annotation.remark.Hack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;
import org.zalando.riptide.autoconfigure.junit.EnvironmentVariables;
import org.zalando.stups.tokens.AccessTokens;
import org.zalando.stups.tokens.ClientCredentials;
import org.zalando.stups.tokens.ClientCredentialsProvider;
import org.zalando.stups.tokens.TokenRefresherConfiguration;
import org.zalando.stups.tokens.UserCredentials;
import org.zalando.stups.tokens.UserCredentialsProvider;

import java.lang.reflect.Field;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(classes = DefaultTestConfiguration.class, webEnvironment = NONE)
@TestPropertySource(properties = "riptide.oauth.access-token-url: http://example.com")
@Component
final class AccessTokensDefaultCredentialsDirectoryTest {

    private static final EnvironmentVariables ENVIRONMENT = new EnvironmentVariables();

    @BeforeAll
    static void setAccessTokenUrl() {

        ENVIRONMENT.set("CREDENTIALS_DIR", Paths.get("src/test/resources").toAbsolutePath().toString());
    }

    @AfterAll
    static void removeAccessTokenUrl() {
        ENVIRONMENT.set("CREDENTIALS_DIR", null);
    }

    @Autowired
    private AccessTokens accessTokens;

    @Test
    void shouldUseCredentialsDirectory() throws NoSuchFieldException, IllegalAccessException {
        final TokenRefresherConfiguration configuration = getConfiguration();

        final ClientCredentialsProvider clientCredentialsProvider = configuration.getClientCredentialsProvider();
        final UserCredentialsProvider userCredentialsProvider = configuration.getUserCredentialsProvider();

        final ClientCredentials client = clientCredentialsProvider.get();
        assertThat(client.getId(), is("id"));
        assertThat(client.getSecret(), is("secret"));

        final UserCredentials user = userCredentialsProvider.get();
        assertThat(user.getUsername(), is("username"));
        assertThat(user.getPassword(), is("password"));
    }

    @Hack
    private TokenRefresherConfiguration getConfiguration() throws NoSuchFieldException, IllegalAccessException {
        final Class<? extends AccessTokens> type = accessTokens.getClass();
        final Field field = type.getSuperclass().getDeclaredField("configuration");
        field.setAccessible(true);
        return (TokenRefresherConfiguration) field.get(accessTokens);
    }

}
