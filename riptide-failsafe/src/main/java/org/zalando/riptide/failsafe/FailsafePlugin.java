package org.zalando.riptide.failsafe;

import lombok.AllArgsConstructor;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Policy;
import net.jodah.failsafe.RetryPolicy;
import org.apiguardian.api.API;
import org.organicdesign.fp.collections.ImList;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Attribute;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;
import org.zalando.riptide.idempotency.IdempotencyPredicate;

import java.util.List;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.organicdesign.fp.StaticImports.vec;
import static org.zalando.riptide.failsafe.TaskDecorator.identity;

@API(status = MAINTAINED)
@AllArgsConstructor(access = PRIVATE)
public final class FailsafePlugin implements Plugin {

    public static final Attribute<Integer> ATTEMPTS = Attribute.generate();

    private final ImList<RequestPolicy> policies;
    private final TaskDecorator decorator;

    public FailsafePlugin() {
        this(vec(), identity());
    }

    public FailsafePlugin withPolicy(final BackupRequest<ClientHttpResponse> policy) {
        return withPolicy(policy, new IdempotencyPredicate());
    }

    public FailsafePlugin withPolicy(final RetryPolicy<ClientHttpResponse> policy) {
        return withPolicy(new RetryRequestPolicy(policy));
    }

    public FailsafePlugin withPolicy(final Policy<ClientHttpResponse> policy) {
        return withPolicy(RequestPolicy.of(policy));
    }

    public FailsafePlugin withPolicy(
            final Policy<ClientHttpResponse> policy,
            final Predicate<RequestArguments> predicate) {
        return withPolicy(RequestPolicy.of(policy, predicate));
    }

    public FailsafePlugin withPolicy(final RequestPolicy policy) {
        return new FailsafePlugin(policies.append(policy), decorator);
    }

    public FailsafePlugin withDecorator(final TaskDecorator decorator) {
        return new FailsafePlugin(policies, decorator);
    }

    @Override
    public RequestExecution aroundAsync(final RequestExecution execution) {
        return arguments -> {
            final List<Policy<ClientHttpResponse>> policies = select(arguments);

            if (policies.isEmpty()) {
                return execution.execute(arguments);
            }

            return Failsafe.with(select(arguments))
                    .getStageAsync(decorator.decorate(context -> execution
                            .execute(withAttempts(arguments, context.getAttemptCount()))));
        };
    }

    private List<Policy<ClientHttpResponse>> select(final RequestArguments arguments) {
        return policies.stream()
                .filter(policy -> policy.applies(arguments))
                .map(policy -> policy.prepare(arguments))
                .collect(toList());
    }

    private RequestArguments withAttempts(
            final RequestArguments arguments,
            final int attempts) {

        if (attempts == 0) {
            return arguments;
        }

        return arguments.withAttribute(ATTEMPTS, attempts);
    }

}
