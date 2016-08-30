package org.zalando.riptide;

import org.springframework.http.client.ClientHttpResponse;
import org.zalando.fauxpas.ThrowingConsumer;

@FunctionalInterface
interface ClientHttpResponseConsumer extends ThrowingConsumer<ClientHttpResponse, Exception> {

}
