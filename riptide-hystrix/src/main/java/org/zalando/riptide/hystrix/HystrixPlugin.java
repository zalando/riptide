package org.zalando.riptide.hystrix;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;
import rx.Observable;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import static com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE;
import static com.netflix.hystrix.HystrixObservableCommand.Setter.withGroupKey;

public final class HystrixPlugin implements Plugin {

    private final SetterFactory factory;

    public HystrixPlugin() {
        this(null);
    }

    public HystrixPlugin(@Nullable final SetterFactory factory) {
        this.factory = Optional.ofNullable(factory).orElse(request ->
                withGroupKey(HystrixCommandGroupKey.Factory.asKey(deriveCommandGroupKey(request)))
                .andCommandKey(HystrixCommandKey.Factory.asKey(deriveCommandKey(request)))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionIsolationStrategy(SEMAPHORE)));
    }

    private static String deriveCommandGroupKey(final RequestArguments arguments) {
        return arguments.getRequestUri().getHost();
    }

    private static String deriveCommandKey(final RequestArguments arguments) {
        return arguments.getMethod() + " " + Optional.ofNullable(arguments.getUriTemplate())
                .orElseGet(arguments.getRequestUri()::getPath);
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        return () -> {
            final HystrixObservableCommand<ClientHttpResponse> command =
                    new HystrixObservableCommand<ClientHttpResponse>(factory.create(arguments)) {

                        @Override
                        protected Observable<ClientHttpResponse> construct() {
                            try {
                                return new CompletableFutureObservable<>(execution.execute());
                            } catch (final IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }

                    };

            return new ObservableCompletableFuture<>(command.observe());
        };
    }

}
