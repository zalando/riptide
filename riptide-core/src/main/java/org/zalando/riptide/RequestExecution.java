package org.zalando.riptide;

import org.apiguardian.api.*;
import org.springframework.http.client.*;

import java.io.*;
import java.util.concurrent.*;

import static org.apiguardian.api.API.Status.*;

@API(status = STABLE)
@FunctionalInterface
public interface RequestExecution {

    CompletableFuture<ClientHttpResponse> execute(RequestArguments arguments) throws IOException;

}
