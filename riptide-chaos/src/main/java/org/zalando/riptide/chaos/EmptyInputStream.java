package org.zalando.riptide.chaos;

import java.io.IOException;
import java.io.InputStream;

final class EmptyInputStream extends InputStream {

    static final InputStream EMPTY = new EmptyInputStream();

    private EmptyInputStream() {

    }

    @Override
    public int read() throws IOException {
        return -1;
    }

}
