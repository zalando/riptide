package org.zalando.riptide.spring;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

final class GzippingHttpRequestInterceptor implements HttpRequestInterceptor {

    @Override
    public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
        if (request instanceof HttpEntityEnclosingRequest) {
            final HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
            final HttpEntity entity = entityRequest.getEntity();
            if (entity != null) {
                final GzipCompressingEntity zippedEntity = new GzipCompressingEntity(entity);
                entityRequest.setEntity(zippedEntity);

                request.removeHeaders(HTTP.CONTENT_ENCODING);
                request.addHeader(zippedEntity.getContentEncoding());

                request.removeHeaders(HTTP.CONTENT_LEN);

                request.removeHeaders(HTTP.TRANSFER_ENCODING);
                request.addHeader(HTTP.TRANSFER_ENCODING, HTTP.CHUNK_CODING);
            }

        }
    }

}
