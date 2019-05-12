package org.zalando.riptide.logbook;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;

final class TeeHttpOutputMessage implements HttpOutputMessage {

    private final HttpOutputMessage message;
    private final OutputStream tee;

    TeeHttpOutputMessage(final HttpOutputMessage message, final OutputStream branch) throws IOException {
        this.message = message;
        this.tee = new TeeOutputStream(message.getBody(), branch);
    }

    @Nonnull
    @Override
    public HttpHeaders getHeaders() {
        return message.getHeaders();
    }

    @Nonnull
    @Override
    public OutputStream getBody() {
        return tee;
    }

}
