package org.zalando.riptide;

import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface RequestExecution {

    CompletableFuture<ClientHttpResponse> execute(RequestArguments arguments) throws IOException;

}
