package org.zalando.riptide.failsafe;

import dev.failsafe.DelayablePolicyConfig;
import dev.failsafe.PolicyConfig;
import dev.failsafe.RetryPolicy;
import dev.failsafe.RetryPolicyBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import dev.failsafe.Policy;
import dev.failsafe.spi.PolicyExecutor;
import org.apiguardian.api.API;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
@Getter
public final class BackupRequest<R> implements Policy<R> {

    private final long delay;
    private final TimeUnit unit;

    @Override
    public PolicyConfig<R> getConfig() {
        //TODO: move to param, add unit -> ChronoUnit conversion
        return RetryPolicy.<R>builder()
                .withDelay(Duration.of (delay, ChronoUnit.SECONDS) )
                .build().getConfig();
    }

    @Override
    @SuppressWarnings("unchecked")
    public PolicyExecutor<R> toExecutor(int policyIndex) {
        return create(policyIndex);
    }

    private PolicyExecutor<R> create(int policyIndex) {
        return new BackupRequestExecutor<>(this, policyIndex);
    }

}
