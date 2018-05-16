package org.zalando.riptide;

import com.google.common.io.ByteStreams;
import com.google.gag.annotation.remark.Hack;
import org.apiguardian.api.API;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClientException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.apiguardian.api.API.Status.STABLE;
import static org.zalando.fauxpas.TryWith.tryWith;

@API(status = STABLE)
public abstract class HttpResponseException extends RestClientException {

    private static final int MAX_BODY_BYTES_TO_READ = 8192;

    private final int rawStatusCode;
    private final String statusText;
    private final HttpHeaders responseHeaders;
    private final byte[] responseBody;

    public HttpResponseException(final String message, final ClientHttpResponse response) throws IOException {
        this(message, response.getRawStatusCode(), response.getStatusText(), response.getHeaders(),
                extractCharset(response), readFromBody(response));
    }

    private HttpResponseException(final String message, final int rawStatusCode, final String statusText,
            final HttpHeaders headers, final Charset charset, final byte[] responseBody) throws IOException {
        super(format(message, responseBody, charset, rawStatusCode, statusText, headers));
        this.rawStatusCode = rawStatusCode;
        this.statusText = statusText;
        this.responseHeaders = headers;
        this.responseBody = responseBody;
    }

    private static byte[] readFromBody(final ClientHttpResponse response) throws IOException {
        return tryWith(response.getBody(), stream -> {
            // needed for spring versions prior to 4.3.14
            if (stream == null) {
                return new byte[0];
            }

            final InputStream input = ByteStreams.limit(stream, MAX_BODY_BYTES_TO_READ);
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            ByteStreams.copy(input, output);
            return output.toByteArray();
        });
    }

    private static Charset extractCharset(final ClientHttpResponse response) {
        return Optional.ofNullable(response.getHeaders().getContentType())
                .map(HttpResponseException::extractCharset)
                .orElse(ISO_8859_1);
    }

    @Hack("MediaType#getCharset is not available prior to Spring 4.3")
    @SuppressWarnings("deprecation")
    private static Charset extractCharset(final MediaType mediaType) {
        return mediaType.getCharset();
    }

    private static String format(final String message, final byte[] body, final Charset charset,
            final int statusCode, final String reasonPhrase, final HttpHeaders headers) {
        return String.format("%s: %d - %s\n%s\n%s", message, statusCode, reasonPhrase, headers, new String(body, charset));
    }

    public int getRawStatusCode() {
        return rawStatusCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public HttpHeaders getResponseHeaders() {
        return responseHeaders;
    }

    public byte[] getResponseBody() {
        return responseBody;
    }

}
