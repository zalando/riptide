package org.zalando.riptide.chaos;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestExecution;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

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
