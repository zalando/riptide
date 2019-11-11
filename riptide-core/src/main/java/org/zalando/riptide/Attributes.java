package org.zalando.riptide;

import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class Attributes {

    /**
     * A number identifying how often this has been attempted already. For the
     * initial request this {@link Attribute attribute} will be absent. Upon the
     * first retry (second attempt) it will be set to {@code 1}.
     */
    public static final Attribute<Integer> RETRIES = Attribute.generate();

    /**
     * Allows to override idempotency detection from a call site by specifying
     * the attribute. This might be useful in situation where the caller knows
     * that a certain operation is idempotent, but it's undetectable using
     * other means.
     *
     * @see <a href="https://nakadi.io/manual.html#/subscriptions/subscription_id/cursors_post">Nakadi API: Endpoint for committing offsets</a>
     */
    public static final Attribute<Boolean> IDEMPOTENT = Attribute.generate();

    private Attributes() {

    }

}
