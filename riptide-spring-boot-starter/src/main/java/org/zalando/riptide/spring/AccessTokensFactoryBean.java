package org.zalando.riptide.spring;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.zalando.riptide.spring.RiptideSettings.Client.OAuth;
import org.zalando.riptide.spring.RiptideSettings.Defaults;
import org.zalando.riptide.spring.RiptideSettings.GlobalOAuth;
import org.zalando.stups.tokens.AccessTokens;
import org.zalando.stups.tokens.AccessTokensBuilder;
import org.zalando.stups.tokens.JsonFileBackedClientCredentialsProvider;
import org.zalando.stups.tokens.JsonFileBackedUserCredentialsProvider;
import org.zalando.stups.tokens.Tokens;

import javax.annotation.Nullable;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

final class AccessTokensFactoryBean extends AbstractFactoryBean<AccessTokens> {

    private AccessTokensBuilder builder;

    AccessTokensFactoryBean(final RiptideSettings settings) {
        final GlobalOAuth oAuth = settings.getOauth();

        final URI accessTokenUrl = getAccessTokenUrl(oAuth);
        @Nullable final Path directory = oAuth.getCredentialsDirectory();
        final TimeSpan connectTimeout = oAuth.getConnectTimeout();
        final TimeSpan socketTimeout = oAuth.getSocketTimeout();

        this.builder = Tokens.createAccessTokensWithUri(accessTokenUrl)
                .usingClientCredentialsProvider(getClientCredentialsProvider(directory))
                .usingUserCredentialsProvider(getUserCredentialsProvider(directory))
                .schedulingPeriod((int) oAuth.getSchedulingPeriod().getAmount())
                .schedulingTimeUnit(oAuth.getSchedulingPeriod().getUnit())
                .connectTimeout((int) connectTimeout.to(TimeUnit.MILLISECONDS))
                .socketTimeout((int) socketTimeout.to(TimeUnit.MILLISECONDS));

        settings.getClients().forEach((id, client) -> {
            @Nullable final OAuth clientOAuth = client.getOauth();

            if (clientOAuth == null) {
                return;
            }

            builder.manageToken(id)
                    .addScopesTypeSafe(clientOAuth.getScopes())
                    .done();
        });
    }

    private JsonFileBackedClientCredentialsProvider getClientCredentialsProvider(@Nullable final Path directory) {
        return directory == null ?
                new JsonFileBackedClientCredentialsProvider() :
                new JsonFileBackedClientCredentialsProvider(directory.resolve("client.json").toFile());
    }

    private JsonFileBackedUserCredentialsProvider getUserCredentialsProvider(@Nullable final Path directory) {
        return directory == null ?
                new JsonFileBackedUserCredentialsProvider() :
                new JsonFileBackedUserCredentialsProvider(directory.resolve("user.json").toFile());
    }

    private URI getAccessTokenUrl(final GlobalOAuth oauth) {
        @Nullable final URI accessTokenUrl = oauth.getAccessTokenUrl();

        checkArgument(accessTokenUrl != null, "" +
                "Neither 'riptide.oauth.access-token-url' nor 'ACCESS_TOKEN_URL' was set, " +
                "but at least one client requires OAuth");

        return accessTokenUrl;
    }

    @Override
    protected AccessTokens createInstance() {
        return builder.start();
    }

    @Override
    public Class<?> getObjectType() {
        return AccessTokens.class;
    }

    @Override
    protected void destroyInstance(final AccessTokens tokens) {
        tokens.stop();
    }

}
