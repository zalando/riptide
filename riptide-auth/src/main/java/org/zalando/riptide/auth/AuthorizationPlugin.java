package org.zalando.riptide.auth;

import com.google.common.collect.ImmutableMultimap;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;

public final class AuthorizationPlugin implements Plugin {

    private final AuthorizationProvider provider;

    public AuthorizationPlugin(final AuthorizationProvider provider) {
        this.provider = provider;
    }

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return arguments -> {
            try {
                return execution.execute(withAuthorizationIfAbsent(arguments));
            } catch (final IOException e) {
                final CompletableFuture<ClientHttpResponse> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        };
    }

    private RequestArguments withAuthorizationIfAbsent(final RequestArguments arguments) throws IOException {
        if (arguments.getHeaders().containsKey(AUTHORIZATION)) {
            return arguments;
        }

        return withAuthorization(arguments);
    }

    private RequestArguments withAuthorization(final RequestArguments arguments) throws IOException {
        return arguments
                .withHeaders(ImmutableMultimap.<String, String>builder()
                        .putAll(arguments.getHeaders())
                        .put(AUTHORIZATION, provider.get())
                        .build());
    }

}
