package org.zalando.riptide;

import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.zalando.fauxpas.TryWith.tryWith;

@SuppressWarnings("serial")
public final class NoRouteException extends RestClientException {

    private static final int MAX_BODY_BYTES_TO_READ = 8192;

    private final ClientHttpResponse response;

    public NoRouteException(final ClientHttpResponse response) throws IOException {
        super(formatMessage(response));
        this.response = response;
    }

    private static String formatMessage(final ClientHttpResponse response) throws IOException {
        return String.format("Unable to dispatch response: %d - %s\n%s\n%s",
                response.getRawStatusCode(), response.getStatusText(), response.getHeaders(),
                readStartOfBody(response));
    }

    private static String readStartOfBody(final ClientHttpResponse response) throws IOException {
        return tryWith(response.getBody(), stream -> {
            if (stream == null) {
                return "";
            }

            final byte[] buffer = new byte[MAX_BODY_BYTES_TO_READ];
            final int read = stream.read(buffer);
            if (read == -1) {
                return "";
            }
            final Charset charset = extractCharset(response);
            return new String(buffer, 0, read, charset);
        });
    }

    private static Charset extractCharset(final ClientHttpResponse response) {
        return Optional.ofNullable(response.getHeaders().getContentType())
                .map(MediaType::getCharset)
                .orElse(ISO_8859_1);
    }

    public ClientHttpResponse getResponse() {
        return response;
    }

}
