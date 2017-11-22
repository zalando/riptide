package org.zalando.riptide.capture;

import java.util.NoSuchElementException;

public final class CaptureException extends NoSuchElementException {

    public CaptureException() {
        super("No value present");
    }

}
