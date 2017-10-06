package org.zalando.riptide.spring;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class GzippingHttpRequestInterceptorTest {

    private final GzippingHttpRequestInterceptor unit = new GzippingHttpRequestInterceptor();
    private final HttpContext context = mock(HttpContext.class);

    @Test
    public void shouldIgnoreRequestWithoutEntity() throws Exception {
        final HttpRequest request = mock(HttpRequest.class);

        unit.process(request, context);

        verifyNoMoreInteractions(request, context);
    }

    @Test
    public void shouldIgnoreRequestWithEntityMissing() throws Exception {
        final HttpEntityEnclosingRequest request = mock(HttpEntityEnclosingRequest.class);

        unit.process(request, context);

        verify(request).getEntity();
        verifyNoMoreInteractions(request, context);
    }

    @Test
    public void shouldWrapEntity() throws Exception {
        final HttpEntityEnclosingRequest request = mock(HttpEntityEnclosingRequest.class);
        final HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContentType()).thenReturn(new BasicHeader(HTTP.CONTENT_TYPE, "application/xml"));
        when(request.getEntity()).thenReturn(entity);

        unit.process(request, context);

        verify(request).setEntity(argThat(allOf(
                instanceOf(GzipCompressingEntity.class),
                hasFeature((GzipCompressingEntity e) -> e.getContentType().getValue(), is("application/xml")))));
    }

    @Test
    public void shouldAdaptContentEncodingHeaders() throws Exception {
        final HttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("GET", "http://example.com");
        request.setEntity(new ByteArrayEntity(new byte[2]));

        request.setHeader(HTTP.CONTENT_ENCODING, "some-encoding");

        unit.process(request, context);

        final List<Header> headers = asList(request.getHeaders(HTTP.CONTENT_ENCODING));
        assertThat(headers, hasSize(1));
        assertThat(headers, hasItem(allOf(
                hasFeature(BasicHeader::getName, is(HTTP.CONTENT_ENCODING)),
                hasFeature(BasicHeader::getValue, is("gzip")))));
    }

    @Test
    public void shouldRemoveContentLengthHeaders() throws Exception {
        final HttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("GET", "http://example.com");
        request.setEntity(new ByteArrayEntity(new byte[2]));

        request.setHeader(HTTP.CONTENT_LEN, "2");

        unit.process(request, context);

        final List<Header> headers = asList(request.getHeaders(HTTP.CONTENT_LEN));
        assertThat(headers, hasSize(0));
    }

    @Test
    public void shouldAdaptTransferEncodingHeaders() throws Exception {
        final HttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("GET", "http://example.com");
        request.setEntity(new ByteArrayEntity(new byte[2]));

        request.setHeader(HTTP.TRANSFER_ENCODING, "some-encoding");

        unit.process(request, context);

        final List<Header> headers = asList(request.getHeaders(HTTP.TRANSFER_ENCODING));
        assertThat(headers, hasSize(1));
        assertThat(headers, hasItem(allOf(
                hasFeature(BasicHeader::getName, is(HTTP.TRANSFER_ENCODING)),
                hasFeature(BasicHeader::getValue, is(HTTP.CHUNK_CODING)))));
    }

}
