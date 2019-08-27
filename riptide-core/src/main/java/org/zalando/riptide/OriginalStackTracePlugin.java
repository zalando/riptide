package org.zalando.riptide;

import org.apiguardian.api.*;

import java.util.function.*;

import static com.google.common.base.Suppliers.*;
import static com.google.common.collect.ObjectArrays.*;
import static org.apiguardian.api.API.Status.*;
import static org.zalando.fauxpas.FauxPas.*;

/**
 * Preserves the original stack traces of failed requests. Requests in Riptide are executed asynchronously by default.
 * That has the unfortunate side-effect that stack traces from exceptions that happen when processing the response will
 * not contain everything that is needed to trace back to the caller.
 * <p>
 * This plugin will modify the stack trace of any thrown exception and appending the stack trace elements of the
 * original stack trace
 */
@API(status = STABLE)
public final class OriginalStackTracePlugin implements Plugin {

    /**
     * {@link Attribute} that allows to access the original stack trace, i.e. the stack trace from the calling thread.
     */
    public static final Attribute<Supplier<StackTraceElement[]>> STACK = Attribute.generate();

    @Override
    public RequestExecution aroundAsync(final RequestExecution execution) {
        return arguments -> {
            final Supplier<StackTraceElement[]> original = keepOriginalStackTrace();

            return execution.execute(arguments.withAttribute(STACK, original))
                    .exceptionally(partially(cause -> {
                        cause.setStackTrace(join(cause, original.get()));
                        throw cause;
                    }));
        };
    }

    private StackTraceElement[] join(final Throwable throwable, final StackTraceElement[] original) {
        return concat(throwable.getStackTrace(), original, StackTraceElement.class);
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
        return memoize(new Exception()::getStackTrace);
    }

}
