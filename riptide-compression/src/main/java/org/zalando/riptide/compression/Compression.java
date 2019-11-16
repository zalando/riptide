package org.zalando.riptide.compression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apiguardian.api.API;

import java.io.IOException;
import java.io.OutputStream;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor(staticName = "of")
@Getter
public final class Compression {
    private final String contentEncoding;
    private final OutputStreamDecorator outputStreamDecorator;

    @FunctionalInterface
    public interface OutputStreamDecorator {
        OutputStream wrap(final OutputStream stream) throws IOException;
    }
}
