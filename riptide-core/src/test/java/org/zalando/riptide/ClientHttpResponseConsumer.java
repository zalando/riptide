package org.zalando.riptide;

import org.springframework.http.client.*;
import org.zalando.fauxpas.*;

@FunctionalInterface
interface ClientHttpResponseConsumer extends ThrowingConsumer<ClientHttpResponse, Exception> {

}
