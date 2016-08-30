package org.zalando.riptide;

import org.springframework.http.HttpEntity;
import org.springframework.http.client.AsyncClientHttpRequest;

import java.io.IOException;

interface MessageWriter {

    <T> void write(final AsyncClientHttpRequest request, final HttpEntity<T> entity) throws IOException;

}
