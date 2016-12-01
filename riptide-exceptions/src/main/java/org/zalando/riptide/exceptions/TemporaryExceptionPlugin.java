package org.zalando.riptide.exceptions;

import com.google.common.base.Throwables;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import javax.net.ssl.SSLHandshakeException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;

import static org.zalando.fauxpas.FauxPas.rethrow;
import static org.zalando.fauxpas.FauxPas.throwingFunction;

public final class TemporaryExceptionPlugin implements Plugin {

    private final Predicate<Throwable> temporary;

    public TemporaryExceptionPlugin() {
        this(InterruptedIOException.class::isInstance,
                SocketException.class::isInstance,
                SSLHandshakeException.class::isInstance);
    }

    @SafeVarargs
    public TemporaryExceptionPlugin(final Predicate<Throwable>... predicates) {
        this(Arrays.asList(predicates));
    }

    public TemporaryExceptionPlugin(final Collection<Predicate<Throwable>> predicates) {
        this.temporary = predicates.stream()
                .reduce(Predicate::or)
                .orElse(throwable -> false);
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        return () -> execution.execute()
                .exceptionally(throwingFunction(this::classify));
    }

    private ClientHttpResponse classify(final Throwable throwable) throws Throwable {
        final Throwable root = Throwables.getRootCause(throwable);

        if (temporary.test(root)) {
            throw new TemporaryException(skipCompletionException(throwable));
        }

        throw throwable;
    }

    /**
     * @see <a href="http://stackoverflow.com/questions/27430255/surprising-behavior-of-java-8-completablefuture-exceptionally-method"/>
     * @param throwable the throwable
     * @return the cause of the given throwable if it's a {@link CompletionException}
     */
    private Throwable skipCompletionException(final Throwable throwable) {
        return throwable instanceof CompletionException ? throwable.getCause() : throwable;
    }

}
