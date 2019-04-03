package org.zalando.riptide.opentracing;

public final class ExtensionFields {

    /**
     * In combination with {@link ExtensionTags#RETRY retry tag}, this field holds the number of the retry attempt.
     */
    public static final String RETRY_NUMBER = "retry_number";

    private ExtensionFields() {

    }

}
