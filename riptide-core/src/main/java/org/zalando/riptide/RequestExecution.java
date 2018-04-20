package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
@FunctionalInterface
public interface RequestExecution {

    CompletableFuture<ClientHttpResponse> execute() throws IOException;

}
