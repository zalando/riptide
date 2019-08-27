package org.zalando.riptide.faults;

import org.apiguardian.api.*;
import org.zalando.riptide.*;

import static org.apiguardian.api.API.Status.*;
import static org.zalando.fauxpas.FauxPas.*;

@API(status = STABLE)
public final class TransientFaultPlugin implements Plugin {

    private final FaultClassifier classifier;

    public TransientFaultPlugin() {
        this(new DefaultFaultClassifier());
    }

    public TransientFaultPlugin(final FaultClassifier classifier) {
        this.classifier = classifier;
    }

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return arguments -> execution.execute(arguments)
                .exceptionally(partially(classifier::classifyExceptionally));
    }

}
