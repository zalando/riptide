package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.ClientHttpRequest;

import java.io.IOException;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
interface MessageWriter {

    <T> void write(ClientHttpRequest request, HttpEntity<T> entity) throws IOException;

}
