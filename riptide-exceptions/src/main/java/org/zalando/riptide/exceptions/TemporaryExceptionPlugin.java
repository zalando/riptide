package org.zalando.riptide.exceptions;

import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

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
        return () -> execution.execute().exceptionally(classifier::classifyExceptionally);
    }

}
