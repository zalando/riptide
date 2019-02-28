package org.zalando.riptide;

import lombok.AllArgsConstructor;

/**
 * Allows to override idempotency detection from a call site by specifying the {@link MethodDetector#IDEMPOTENT}
 * attribute. This might be useful in situation where the caller knows that a certain operation is idempontent, but
 * it's undetectable using other means.
 *
 * @see MethodDetector#IDEMPOTENT
 * @see <a href="https://nakadi.io/manual.html#/subscriptions/subscription_id/cursors_post">Nakadi API: Endpoint for committing offsets</a>
 */
@AllArgsConstructor
public final class OverrideIdempotentMethodDetector implements MethodDetector {

    @Override
    public boolean test(final RequestArguments arguments) {
        return arguments.getAttribute(IDEMPOTENT).orElse(Boolean.FALSE);
    }

}
