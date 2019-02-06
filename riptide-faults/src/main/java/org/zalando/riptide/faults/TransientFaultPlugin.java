package org.zalando.riptide.faults;

import org.apiguardian.api.API;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestExecution;

import static org.apiguardian.api.API.Status.STABLE;
import static org.zalando.fauxpas.FauxPas.partially;

@API(status = STABLE)
public final class TransientFaultPlugin implements Plugin {

    private final FaultClassifier classifier;

    public TransientFaultPlugin() {
        this(FaultClassifier.createDefault());
    }

    public TransientFaultPlugin(final FaultClassifier classifier) {
        this.classifier = classifier;
    }

    @Override
    public RequestExecution beforeDispatch(final RequestExecution execution) {
        return arguments -> execution.execute(arguments).exceptionally(partially(classifier::classifyExceptionally));
    }

}
