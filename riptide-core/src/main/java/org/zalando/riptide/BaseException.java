package org.zalando.riptide;

import com.google.common.io.ByteStreams;
import com.google.gag.annotation.remark.Hack;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClientResponseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

// TODO rename
public abstract class BaseException extends RestClientResponseException {

    private static final int MAX_BODY_BYTES_TO_READ = 8192;

    public BaseException(final String message, final ClientHttpResponse response) throws IOException {
        this(message, readFromBody(response), extractCharset(response),
                response.getStatusText(), response.getRawStatusCode(), response.getHeaders());
    }

    private BaseException(final String message, final byte[] body, final Charset charset, final String statusText,
            final int rawStatusCode, final HttpHeaders headers) throws IOException {
        super(format(message, body, charset, rawStatusCode, statusText, headers), rawStatusCode, statusText, headers,
                body, charset);
    }

    private static byte[] readFromBody(final ClientHttpResponse response) throws IOException {
        if (response.getBody() == null) {
            return new byte[0];
        }
        final InputStream input = ByteStreams.limit(response.getBody(), MAX_BODY_BYTES_TO_READ);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteStreams.copy(input, output);
        return output.toByteArray();
    }

    private static Charset extractCharset(final ClientHttpResponse response) {
        return Optional.ofNullable(response.getHeaders().getContentType())
                .map(BaseException::extractCharset)
                .orElse(ISO_8859_1);
    }

    @Hack("MediaType#getCharset is not available prior to Spring 4.3")
    @SuppressWarnings("deprecation")
    private static Charset extractCharset(final MediaType mediaType) {
        return mediaType.getCharSet();
    }

    private static String format(final String message, final byte[] body, final Charset charset,
            final int statusCode, final String reasonPhrase, final HttpHeaders headers) throws IOException {
        return String.format("%s: %d - %s\n%s\n%s", message, statusCode, reasonPhrase, headers, new String(body, charset));
    }

}
