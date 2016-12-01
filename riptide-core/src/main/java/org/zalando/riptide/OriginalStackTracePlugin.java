package org.zalando.riptide;

import org.springframework.http.client.ClientHttpResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.google.common.collect.ObjectArrays.concat;
import static org.zalando.fauxpas.FauxPas.throwingFunction;

public final class OriginalStackTracePlugin implements Plugin {

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        return () -> {
            final CompletableFuture<ClientHttpResponse> future = execution.execute();
            // let's do the "heavy" stack trace work while the request is already on its way
            final Supplier<StackTraceElement[]> original = keepOriginalStackTrace();
            return future.exceptionally(throwingFunction(cause -> {
                cause.setStackTrace(concat(cause.getStackTrace(), original.get(), StackTraceElement.class));
                throw cause;
            }));
        };
    }

    /**
     * A good way to store a stacktrace away efficiently is to simply construct an exception. Later, if you
     * want to inspect the stacktrace call exception.getStackTrace() which will do the slow work of
     * resolving the stack frames to methods.
     * <p>
     * <a href="http://stackoverflow.com/a/4377609/232539>What is the proper way to keep track of the original stack trace in a newly created Thread?</a>
     */
    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private Supplier<StackTraceElement[]> keepOriginalStackTrace() {
        return new Exception()::getStackTrace;
    }

}
