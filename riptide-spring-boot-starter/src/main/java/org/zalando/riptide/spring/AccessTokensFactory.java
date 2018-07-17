package org.zalando.riptide.spring;

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

@SuppressWarnings("unused")
final class AccessTokensFactory {

    private AccessTokensFactory() {

    }

    public static AccessTokens createAccessTokens(final RiptideProperties properties) {
        final RiptideProperties.GlobalOAuth oAuth = properties.getOauth();

        final URI accessTokenUrl = getAccessTokenUrl(oAuth);
        @Nullable final Path directory = oAuth.getCredentialsDirectory();
        final TimeSpan connectTimeout = oAuth.getConnectTimeout();
        final TimeSpan socketTimeout = oAuth.getSocketTimeout();

        final AccessTokensBuilder builder = Tokens.createAccessTokensWithUri(accessTokenUrl)
                .usingClientCredentialsProvider(getClientCredentialsProvider(directory))
                .usingUserCredentialsProvider(getUserCredentialsProvider(directory))
                .schedulingPeriod((int) oAuth.getSchedulingPeriod().getAmount())
                .schedulingTimeUnit(oAuth.getSchedulingPeriod().getUnit())
                .connectTimeout((int) connectTimeout.to(TimeUnit.MILLISECONDS))
                .socketTimeout((int) socketTimeout.to(TimeUnit.MILLISECONDS));

        properties.getClients().forEach((id, client) -> {
            @Nullable final RiptideProperties.Client.OAuth clientOAuth = client.getOauth();

            if (clientOAuth == null) {
                return;
            }

            builder.manageToken(id)
                    .addScopesTypeSafe(clientOAuth.getScopes())
                    .done();
        });

        return builder.start();
    }

    private static JsonFileBackedClientCredentialsProvider getClientCredentialsProvider(@Nullable final Path directory) {
        return directory == null ?
                new JsonFileBackedClientCredentialsProvider() :
                new JsonFileBackedClientCredentialsProvider(directory.resolve("client.json").toFile());
    }

    private static JsonFileBackedUserCredentialsProvider getUserCredentialsProvider(@Nullable final Path directory) {
        return directory == null ?
                new JsonFileBackedUserCredentialsProvider() :
                new JsonFileBackedUserCredentialsProvider(directory.resolve("user.json").toFile());
    }

    private static URI getAccessTokenUrl(final RiptideProperties.GlobalOAuth oauth) {
        @Nullable final URI accessTokenUrl = oauth.getAccessTokenUrl();

        checkArgument(accessTokenUrl != null, "" +
                "Neither 'riptide.oauth.access-token-url' nor 'ACCESS_TOKEN_URL' was set, " +
                "but at least one client requires OAuth");

        return accessTokenUrl;
    }

}
