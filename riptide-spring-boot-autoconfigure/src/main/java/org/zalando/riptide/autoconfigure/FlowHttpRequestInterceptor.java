package org.zalando.riptide.autoconfigure;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apiguardian.api.API;
import org.zalando.opentracing.flowid.Flow;

import static org.apiguardian.api.API.Status.STABLE;

/**
 * This class is adapted from org.zalando.opentracing.flowid.httpclient.FlowHttpRequestInterceptor.
 * The original class is based on Apache HttpClient 4.x, but we use Apache HttpClient 5.x.
 */
@API(status = STABLE)
public final class FlowHttpRequestInterceptor implements HttpRequestInterceptor {

    private final Flow flow;

    public FlowHttpRequestInterceptor(final Flow flow) {
        this.flow = flow;
    }

    @Override
    public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) {
        flow.writeTo(httpRequest::addHeader);
    }
}