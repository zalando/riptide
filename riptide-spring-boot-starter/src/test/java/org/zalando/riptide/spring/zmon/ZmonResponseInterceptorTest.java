package org.zalando.riptide.spring.zmon;

import org.apache.http.HttpException;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;
import org.zalando.zmon.actuator.metrics.MetricsWrapper;

import java.io.IOException;

import static org.apache.http.HttpVersion.HTTP_1_1;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.longThat;

public class ZmonResponseInterceptorTest {

    private final MetricsWrapper metricsWrapper = mock(MetricsWrapper.class);
    private final ZmonResponseInterceptor unit = new ZmonResponseInterceptor(metricsWrapper);

    @Test
    public void shouldIgnoreEmptyContext() throws IOException, HttpException {
        final HttpContext context = mock(HttpContext.class);
        when(context.getAttribute(Timing.ATTRIBUTE)).thenReturn(null);

        unit.process(new BasicHttpResponse(HTTP_1_1, 200, "ok"), context);

        verifyNoMoreInteractions(metricsWrapper);
    }

    @Test
    public void shouldRecordMetadata() throws IOException, HttpException {
        final HttpContext context = mock(HttpContext.class);
        when(context.getAttribute(Timing.ATTRIBUTE)).thenReturn(new Timing("GET", "host", 1L));

        unit.process(new BasicHttpResponse(HTTP_1_1, 200, "ok"), context);

        verify(metricsWrapper).recordBackendRoundTripMetrics(eq("GET"), eq("host"), eq(200), anyLong());
    }

    @Test
    public void shouldRecordTiming() throws IOException, HttpException {
        final HttpContext context = mock(HttpContext.class);
        when(context.getAttribute(Timing.ATTRIBUTE)).thenReturn(new Timing("GET", "host", 0L));

        unit.process(new BasicHttpResponse(HTTP_1_1, 200, "ok"), context);

        final Long l = 0L;
        verify(metricsWrapper).recordBackendRoundTripMetrics(anyString(), anyString(), anyInt(), longThat(greaterThan(0L)));
    }


}
