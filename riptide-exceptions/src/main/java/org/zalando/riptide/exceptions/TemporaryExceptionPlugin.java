package org.zalando.riptide.exceptions;

import com.google.common.base.Throwables;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import java.util.concurrent.CompletionException;

import static org.zalando.fauxpas.FauxPas.throwingFunction;

public final class TemporaryExceptionPlugin implements Plugin {

    private final ExceptionClassifier classifier;

    public TemporaryExceptionPlugin() {
        this(ExceptionClassifier.createDefault());
    }

    public TemporaryExceptionPlugin(final ExceptionClassifier classifier) {
        this.classifier = classifier;
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        return () -> execution.execute()
                .exceptionally(throwingFunction(this::classify));
    }

    private ClientHttpResponse classify(final Throwable throwable) throws Throwable {
        final Throwable root = Throwables.getRootCause(throwable);

        if (isTemporary(root)) {
            throw new TemporaryException(skipCompletionException(throwable));
        }

        throw throwable;
    }

    private boolean isTemporary(final Throwable throwable) {
        return classifier.classify(throwable) == Classification.TEMPORARY;
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
