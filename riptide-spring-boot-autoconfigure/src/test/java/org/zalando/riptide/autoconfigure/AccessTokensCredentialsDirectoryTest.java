package org.zalando.riptide.autoconfigure;

import com.google.gag.annotation.remark.Hack;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.stups.tokens.AccessTokens;
import org.zalando.stups.tokens.ClientCredentials;
import org.zalando.stups.tokens.ClientCredentialsProvider;
import org.zalando.stups.tokens.TokenRefresherConfiguration;
import org.zalando.stups.tokens.UserCredentials;
import org.zalando.stups.tokens.UserCredentialsProvider;

import java.lang.reflect.Field;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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
