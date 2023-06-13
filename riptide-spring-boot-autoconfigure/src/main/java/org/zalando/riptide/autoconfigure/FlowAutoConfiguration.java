package org.zalando.riptide.autoconfigure;

import io.opentracing.Tracer;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.zalando.opentracing.flowid.Flow;

public class FlowAutoConfiguration {

    @Bean
    public Flow flow(final Tracer tracer) {
        return Flow.create(tracer);
    }

    static class FlowHttpRequestInterceptor implements HttpRequestInterceptor {
        private final Flow flow;

        public FlowHttpRequestInterceptor(final Flow flow) {
            this.flow = flow;
        }

        @Override
        public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) {
            this.flow.writeTo(httpRequest::addHeader);
        }
    }

    @Bean
    @ConditionalOnMissingBean({FlowHttpRequestInterceptor.class})
    public FlowHttpRequestInterceptor flowHttpRequestInterceptor(final Flow flow) {
        return new FlowHttpRequestInterceptor(flow);
    }
}
