package org.zalando.riptide.chaos;

import lombok.*;
import lombok.extern.slf4j.*;
import org.apiguardian.api.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.util.*;
import org.zalando.riptide.*;

import javax.annotation.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static org.apiguardian.api.API.Status.*;
import static org.zalando.fauxpas.FauxPas.*;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
@Slf4j
public final class ErrorResponseInjection implements FailureInjection {

    private final Probability probability;
    private final List<HttpStatus> statuses;

    @Override
    public RequestExecution inject(final RequestExecution execution) {
        if (probability.test()) {
            return arguments ->
                    execution.execute(arguments).thenApply(throwingFunction(original -> {
                        if (original.getStatusCode().isError()) {
                            // only inject error response if not failed already
                            return original;
                        }

                        try {
                            final HttpStatus status = choose();
                            log.debug("Injecting '{}' error response", status);
                            return new ErrorClientHttpResponse(status);
                        } finally {
                            original.close();
                        }
                    }));
        }

        return execution;
    }

    private HttpStatus choose() {
        return statuses.get(ThreadLocalRandom.current().nextInt(statuses.size()));
    }

    @AllArgsConstructor
    private static class ErrorClientHttpResponse implements ClientHttpResponse {

        private final HttpStatus status;

        @Nonnull
        @Override
        public HttpStatus getStatusCode() {
            return status;
        }

        @Override
        public int getRawStatusCode() {
            return status.value();
        }

        @Nonnull
        @Override
        public String getStatusText() {
            return status.getReasonPhrase();
        }

        @Nonnull
        @Override
        public HttpHeaders getHeaders() {
            return new HttpHeaders();
        }

        @Nonnull
        @Override
        public InputStream getBody() {
            return StreamUtils.emptyInput();
        }

        @Override
        public void close() {
            // nothing to close
        }

    }

}
