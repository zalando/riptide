package org.zalando.riptide.opentracing;

import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class ExtensionFields {

    /**
     * In combination with {@link ExtensionTags#RETRY retry tag}, this field holds the number of the retry attempt.
     */
    public static final String RETRY_NUMBER = "retry_number";

    private ExtensionFields() {

    }

}
