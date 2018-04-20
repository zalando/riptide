package org.zalando.riptide.capture;

import org.apiguardian.api.API;

import java.util.NoSuchElementException;

import static org.apiguardian.api.API.Status.MAINTAINED;

@API(status = MAINTAINED)
public final class CaptureException extends NoSuchElementException {

    public CaptureException() {
        super("No value present");
    }

}
