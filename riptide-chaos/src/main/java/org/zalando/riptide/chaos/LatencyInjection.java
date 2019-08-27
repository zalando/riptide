package org.zalando.riptide.chaos;

import lombok.*;
import lombok.extern.slf4j.*;
import org.apiguardian.api.*;
import org.zalando.riptide.*;

import java.time.*;

import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
@Slf4j
public final class LatencyInjection implements FailureInjection {

    private final Sleeper sleepy = new Sleeper();

    private final Probability probability;
    private final Clock clock;
    private final Duration delay;

    @Override
    public RequestExecution inject(final RequestExecution execution) {
        if (probability.test()) {
            return arguments -> {
                final Instant start = clock.instant();
                return execution.execute(arguments).whenComplete((response, failure) -> {
                    final Instant end = clock.instant();
                    final Duration duration = Duration.between(start, end);
                    final Duration rest = delay.minus(duration);

                    if (rest.isNegative()) {
                        // only inject latency if not delayed already
                        return;
                    }

                    log.debug("Injecting latency of '{}' milliseconds", rest.toMillis());
                    sleepy.sleep(rest);
                });
            };
        }
        
        return execution;
    }

}
