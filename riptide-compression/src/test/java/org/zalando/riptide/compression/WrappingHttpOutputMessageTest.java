package org.zalando.riptide.compression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.zalando.riptide.compression.Compression.OutputStreamDecorator;

import java.io.IOException;
import java.io.OutputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class WrappingHttpOutputMessageTest {

    private final HttpOutputMessage message = mock(HttpOutputMessage.class);
    private final OutputStreamDecorator wrapper = mock(OutputStreamDecorator.class);
    private final WrappingHttpOutputMessage unit = new WrappingHttpOutputMessage(message, wrapper);

    @BeforeEach
    void setUp() throws IOException {
        final OutputStream body = mock(OutputStream.class);
        when(message.getBody()).thenReturn(body);
        when(message.getHeaders()).thenReturn(new HttpHeaders());
    }

    @Test
    void shouldDelegateHeaders() {
        assertThat(unit.getHeaders(), equalTo(message.getHeaders()));
    }

    @Test
    void shouldReturnWrapped() throws IOException {
        final OutputStream wrappedStream = mock(OutputStream.class);
        when(wrapper.wrap(any())).thenReturn(wrappedStream);

        assertThat(unit.getBody(), equalTo(wrappedStream));
        assertThat(unit.getBody(), equalTo(wrappedStream));
    }

    @Test
    void shouldWrapOnFirstAccessOnly() throws IOException {
        final OutputStream wrappedStream = mock(OutputStream.class);
        when(wrapper.wrap(any())).thenReturn(wrappedStream);

        unit.getBody();
        unit.getBody();

        verify(wrapper).wrap(message.getBody());
        verifyNoMoreInteractions(wrapper);
    }

    @Test
    void shouldNotCloseStreamIfNeverAccessed() throws IOException {
        final OutputStream wrappedStream = mock(OutputStream.class);
        when(wrapper.wrap(any())).thenReturn(wrappedStream);

        unit.close();

        verifyNoInteractions(wrappedStream);
    }
}
