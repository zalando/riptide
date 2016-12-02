package org.zalando.riptide.exceptions;

import com.google.common.base.Throwables;
import lombok.SneakyThrows;

import java.util.concurrent.CompletionException;
import java.util.function.Predicate;

final class DefaultExceptionClassifier implements ExceptionClassifier {

    private final Predicate<Throwable> isTemporary;

    public DefaultExceptionClassifier(final Predicate<Throwable> isTemporary) {
        this.isTemporary = isTemporary;
    }

    @Override
    public Throwable classify(final Throwable throwable) {
        final Throwable root = Throwables.getRootCause(throwable);

        if (isTemporary.test(root)) {
            return new TemporaryException(throwable);
        }

        return throwable;
    }

    @Override
    @SneakyThrows
    public <T> T classifyExceptionally(final Throwable throwable) {
        throw classify(skipCompletionException(throwable));
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
