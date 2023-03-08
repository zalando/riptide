package org.zalando.riptide.failsafe;

import dev.failsafe.Policy;
import dev.failsafe.PolicyConfig;
import dev.failsafe.spi.PolicyExecutor;
import lombok.Getter;
import org.apiguardian.api.API;

import java.util.concurrent.TimeUnit;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@Getter
public final class BackupRequest<R> implements Policy<R> {

    private final long delay;
    private final TimeUnit unit;
    private final PolicyConfig<R> config = new PolicyConfig<R>() {
    };

    public BackupRequest(long delay, TimeUnit unit) {
        this.delay = delay;
        this.unit = unit;
    }

    @Override
    public PolicyExecutor<R> toExecutor(int policyIndex) {
        return create(policyIndex);
    }

    private PolicyExecutor<R> create(int policyIndex) {
        return new BackupRequestExecutor<>(this, policyIndex);
    }

}
