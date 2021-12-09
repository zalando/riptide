package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.common.AttributeKey;

public class ExtensionAttributes {
    public static final AttributeKey<String> HTTP_PATH = AttributeKey.stringKey("http.path");
    public static final AttributeKey<Boolean> RETRY = AttributeKey.booleanKey("retry");
    public static final AttributeKey<String> PEER_HOST = AttributeKey.stringKey("peer.hostname");
    public static final AttributeKey<Long> RETRY_NUMBER = AttributeKey.longKey("retry_number");

    private ExtensionAttributes() {
    }
}
