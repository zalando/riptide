package org.zalando.riptide.chaos;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apiguardian.api.API;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestExecution;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.fauxpas.FauxPas.throwingFunction;
import static org.zalando.riptide.chaos.EmptyInputStream.EMPTY;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
@Slf4j
public final class ErrorResponseInjection implements FailureInjection {

    private final Probability probability;
    private final List<HttpStatus> statuses;

    @Override
    public RequestExecution inject(final RequestExecution execution) {
        if (probability.test()) {
            return arguments -> execution.execute(arguments)
                    .thenApply(throwingFunction(this::injectIfNecessary));
        }

        return execution;
    }

    private ClientHttpResponse injectIfNecessary(
            final ClientHttpResponse response) throws IOException {

        final HttpStatus statusCode = response.getStatusCode();

        if (isError(statusCode)) {
            // only inject error response if not failed already
            return response;
        }

        try {
            final HttpStatus status = choose();
            log.debug("Injecting '{}' error response", status);
            return new ErrorClientHttpResponse(status);
        } finally {
            response.close();
        }
    }

    private boolean isError(final HttpStatus status) {
        switch (status.series()) {
            case CLIENT_ERROR:
            case SERVER_ERROR:
                return true;
            default:
                return false;
        }
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
            return EMPTY;
        }

        @Override
        public void close() {
            // nothing to close
        }

    }

}
