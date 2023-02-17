package org.zalando.riptide.failsafe;

import lombok.AllArgsConstructor;
import dev.failsafe.Failsafe;
import dev.failsafe.Policy;
import dev.failsafe.function.ContextualSupplier;
import org.apiguardian.api.API;
import org.organicdesign.fp.collections.ImList;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.organicdesign.fp.StaticImports.vec;
import static org.zalando.riptide.Attributes.RETRIES;

@API(status = MAINTAINED)
@AllArgsConstructor(access = PRIVATE)
public final class FailsafePlugin implements Plugin {

    private final ImList<RequestPolicy> policies;
    private final ImList<TaskDecorator> decorators;

    public FailsafePlugin() {
        this(vec(), vec());
    }

    public FailsafePlugin withPolicy(final Policy<ClientHttpResponse> policy) {
        return withPolicy(RequestPolicies.of(policy));
    }

    public FailsafePlugin withPolicy(
            final Policy<ClientHttpResponse> policy,
            final Predicate<RequestArguments> predicate) {
        return withPolicy(RequestPolicies.of(policy, predicate));
    }

    public FailsafePlugin withPolicy(final RequestPolicy policy) {
        return new FailsafePlugin(policies.append(policy), decorators);
    }

    public FailsafePlugin withDecorator(final TaskDecorator decorator) {
        return new FailsafePlugin(policies, decorators.append(decorator));
    }

    @Override
    public RequestExecution aroundAsync(final RequestExecution execution) {
        return arguments -> {
            final List<Policy<ClientHttpResponse>> policies = select(arguments);

            if (policies.isEmpty()) {
                return execution.execute(arguments);
            }

            return Failsafe.with(select(arguments))
                    .getStageAsync(decorate(execution, arguments));
        };
    }

    private ContextualSupplier<ClientHttpResponse, CompletionStage<ClientHttpResponse>> decorate(
            final RequestExecution execution, final RequestArguments arguments) {

        final TaskDecorator decorator = TaskDecorator.composite(decorators);
        return decorator.decorate(context -> execution
                .execute(withAttempts(arguments, context.getAttemptCount())));
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

        return arguments.withAttribute(RETRIES, attempts);
    }

}
