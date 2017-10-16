package org.zalando.riptide.spring.zmon;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;
import org.zalando.zmon.actuator.metrics.MetricsWrapper;

import java.io.IOException;

import static java.lang.System.currentTimeMillis;

public class ZmonResponseInterceptor implements HttpResponseInterceptor {

    private final MetricsWrapper metricsWrapper;

    public ZmonResponseInterceptor(final MetricsWrapper metricsWrapper) {
        this.metricsWrapper = metricsWrapper;
    }

    @Override
    public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
        final long endTime = currentTimeMillis();
        final Timing timing = (Timing) context.getAttribute(Timing.ATTRIBUTE);
        if (timing != null) {
            metricsWrapper.recordBackendRoundTripMetrics(timing.getMethod(),
                    timing.getHost(),
                    getStatusCode(response),
                    endTime - timing.getStartTime());
        }
    }

    private int getStatusCode(final HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

}
