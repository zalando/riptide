package org.zalando.riptide.httpclient;

import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory.*;

final class BufferingApacheClientHttpRequestFactoryTest extends AbstractApacheClientHttpRequestFactoryTest {

    @Override
    Mode getMode() {
        return Mode.BUFFERING;
    }

}
