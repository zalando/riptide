package org.zalando.riptide.logbook;

import lombok.*;

import java.io.*;

import static org.zalando.fauxpas.TryWith.*;

@AllArgsConstructor
final class TeeOutputStream extends OutputStream {

    private final OutputStream original;
    private final OutputStream branch;

    @Override
    public void write(final byte[] b) throws IOException {
        original.write(b);
        branch.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        original.write(b, off, len);
        branch.write(b, off, len);
    }

    @Override
    public void write(final int b) throws IOException {
        original.write(b);
        branch.write(b);
    }

    @Override
    public void flush() throws IOException {
        original.flush();
        branch.flush();
    }

    @Override
    public void close() {
        tryWith(original, branch, (ignored, ignoredToo) -> {
            // nothing to do
        });
    }

}
