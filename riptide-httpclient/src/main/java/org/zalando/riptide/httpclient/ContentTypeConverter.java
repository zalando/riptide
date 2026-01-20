package org.zalando.riptide.httpclient;

import org.apache.hc.core5.http.ContentType;
import org.springframework.http.MediaType;

public class ContentTypeConverter {

    private ContentTypeConverter() {}

    public static ContentType toContentType(MediaType mediaType) {
        return ContentType.create(mediaType.getType() + "/" + mediaType.getSubtype(), mediaType.getCharset());
    }
}
