package org.zalando.riptide.httpclient;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class ContentTypeConverterTest {

    @Test
    void shouldConvertMediaTypeWithCharset() {
        var contentType = ContentTypeConverter.toContentType(new MediaType(MediaType.TEXT_XML, StandardCharsets.UTF_8));
        assertThat(contentType.getCharset(), is(StandardCharsets.UTF_8));
        assertThat(contentType.getMimeType(), is("text/xml"));
    }

    @Test
    void shouldConvertMediaTypeWithoutCharset() {
        var contentType = ContentTypeConverter.toContentType(MediaType.TEXT_XML);
        assertThat(contentType.getCharset(), is(nullValue()));
        assertThat(contentType.getMimeType(), is("text/xml"));
    }

}
