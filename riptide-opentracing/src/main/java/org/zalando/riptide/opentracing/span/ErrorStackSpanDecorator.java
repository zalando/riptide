package org.zalando.riptide.opentracing.span;

import com.google.common.annotations.*;
import io.opentracing.*;
import io.opentracing.log.*;
import org.zalando.riptide.*;

import javax.annotation.*;
import java.io.*;

import static java.util.Collections.*;
import static org.zalando.riptide.OriginalStackTracePlugin.*;

/**
 * Sets the <code>stack</code> span log.
 *
 * @see <a href="https://opentracing.io/specification/conventions/#log-fields-table">Standard Log Fields</a>
 */
public final class ErrorStackSpanDecorator implements SpanDecorator {

    private final StackRenderer renderer = new StackRenderer();

    @Override
    public void onError(final Span span, final RequestArguments arguments, final Throwable error) {
        final String stack = arguments.getAttribute(STACK)
                .map(original -> renderer.render(error, original.get()))
                .orElseGet(() -> renderer.render(error));

        span.log(singletonMap(Fields.STACK, stack));
    }

    @VisibleForTesting
    static final class StackRenderer {

        private final StackTraceElement[] empty = new StackTraceElement[0];

        String render(final Throwable throwable) {
            return render(throwable, empty);
        }

        /**
         * @see Throwable#printStackTrace(PrintWriter)
         * @param throwable the error
         */
        String render(final Throwable throwable, final StackTraceElement[] original) {
            final StringBuilder output = new StringBuilder(2048);
            printStackTrace(throwable, original, output);
            return output.toString();
        }

        private void printStackTrace(final Throwable throwable, final StringBuilder output) {
            printStackTrace(throwable, empty, output);
        }

        private void printStackTrace(final Throwable throwable, final StackTraceElement[] original, final StringBuilder output) {
            output.append(throwable).append("\n");
            print(throwable.getStackTrace(), output);
            print(original, output);

            @Nullable final Throwable cause = throwable.getCause();

            if (cause != null) {
                output.append("Caused by: ");
                printStackTrace(cause, output);
            }
        }

        private void print(final StackTraceElement[] elements, final StringBuilder output) {
            for (final StackTraceElement element : elements) {
                output.append("\tat ").append(element.toString()).append("\n");
            }
        }

    }

}
