package org.zalando.riptide.httpclient;

import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory.Mode;

final class StreamingApacheClientHttpRequestFactoryTest extends AbstractApacheClientHttpRequestFactoryTest {

    @Override
    Mode getMode() {
        return Mode.STREAMING;
    }

}
