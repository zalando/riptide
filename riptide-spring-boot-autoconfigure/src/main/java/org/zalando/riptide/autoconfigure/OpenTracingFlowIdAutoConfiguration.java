package org.zalando.riptide.autoconfigure;

import io.opentracing.Tracer;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apiguardian.api.API;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.opentracing.flowid.Flow;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.STABLE;

/**
 * This class is adapted from org.zalando.opentracing.flowid.autoconfigure.OpenTracingFlowIdAutoConfiguration.
 * The original class is based on org.zalando.opentracing.flowid.httpclient.FlowHttpRequestInterceptor,
 * which uses Apache HttpClient 4.x.
 * We use org.zalando.riptide.autoconfigure.FlowHttpRequestInterceptor, that is adapted to Apache HttpClient 5.x.
 */
@API(status = STABLE)
@AutoConfiguration
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(name = {
        "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration",
        "io.opentracing.contrib.spring.web.starter.ServerTracingAutoConfiguration"
})
public class OpenTracingFlowIdAutoConfiguration {

    @API(status = INTERNAL)
    @Bean
    public Flow flow(final Tracer tracer) {
        return Flow.create(tracer);
    }

    @API(status = INTERNAL)
    @Configuration
    @ConditionalOnClass(HttpClient.class)
    @ConditionalOnMissingBean(FlowHttpRequestInterceptor.class)
    @ConditionalOnProperty(name = "opentracing.flowid.httpclient.enabled", havingValue = "true", matchIfMissing = true)
    static class OpenTracingFlowIdHttpClientAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean(FlowHttpRequestInterceptor.class)
        public FlowHttpRequestInterceptor flowHttpRequestInterceptor(final Flow flow) {
            return new FlowHttpRequestInterceptor(flow);
        }

    }

}
