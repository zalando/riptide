package org.zalando.riptide.spring.zmon;

import org.apache.http.HttpException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;

import java.io.IOException;

import static java.net.URI.create;
import static org.apache.http.client.methods.HttpRequestWrapper.wrap;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class ZmonRequestInterceptorTest {

    private final ZmonRequestInterceptor unit = new ZmonRequestInterceptor();

    @Test
    public void shouldStoreTimingContextOnRequest() throws IOException, HttpException {
        final HttpContext context = mock(HttpContext.class);

        unit.process(new HttpGet("http://www.example.com/path"), context);

        verify(context).setAttribute(eq(Timing.ATTRIBUTE), argThat(allOf(
                hasFeature(Timing::getHost, is("www.example.com")),
                hasFeature(Timing::getMethod, is("GET")),
                hasFeature(Timing::getStartTime, is(greaterThan(1L))))));
    }

    @Test
    public void shouldStoreHostWithPortOnRequest() throws IOException, HttpException {
        final HttpContext context = mock(HttpContext.class);

        unit.process(new HttpGet("http://www.example.com:80/path"), context);

        verify(context).setAttribute(eq(Timing.ATTRIBUTE), argThat(hasFeature(Timing::getHost, is("www.example.com:80"))));
    }

    @Test
    public void shouldStoreHostOnWrappedRequest() throws IOException, HttpException {
        final HttpContext context = mock(HttpContext.class);

        final HttpRequestWrapper innerRequest = wrap(new HttpGet("http://www.example.com:80/path"));
        final HttpRequestWrapper outerRequest = wrap(innerRequest);
        innerRequest.setURI(create("/path"));
        outerRequest.setURI(create("/path"));
        unit.process(outerRequest, context);

        verify(context).setAttribute(eq(Timing.ATTRIBUTE), argThat(hasFeature(Timing::getHost, is("www.example.com:80"))));
    }

    @Test
    public void shouldIgnoreMalformedRequest() throws IOException, HttpException {
        final HttpContext context = mock(HttpContext.class);

        unit.process(new HttpGet("http://www.example:80.com/path"), context);

        verifyNoMoreInteractions(context);
    }


}
