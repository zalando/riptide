package org.zalando.riptide.failsafe;

import dev.failsafe.PolicyConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import dev.failsafe.Policy;
import dev.failsafe.spi.PolicyExecutor;
import org.apiguardian.api.API;

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
        //TODO: impl
        return null;
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
