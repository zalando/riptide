package org.zalando.riptide.hystrix;

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

public final class HystrixPlugin implements Plugin {

    private final SetterFactory factory;

    public HystrixPlugin() {
        this(null);
    }

    public HystrixPlugin(@Nullable final SetterFactory factory) {
        this.factory = Optional.ofNullable(factory).orElseGet(DefaultSetterFactory::new);
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
