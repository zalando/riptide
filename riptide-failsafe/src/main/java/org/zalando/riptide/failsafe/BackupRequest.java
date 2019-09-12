package org.zalando.riptide.failsafe;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.jodah.failsafe.AbstractExecution;
import net.jodah.failsafe.Policy;
import net.jodah.failsafe.PolicyExecutor;
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
    @SuppressWarnings("unchecked")
    public PolicyExecutor<Policy<R>> toExecutor(
            final AbstractExecution execution) {
        return (PolicyExecutor<Policy<R>>) create(execution);
    }

    private PolicyExecutor<? extends Policy<R>> create(
            final AbstractExecution execution) {
        return new BackupRequestExecutor<R>(this, execution);
    }

}
