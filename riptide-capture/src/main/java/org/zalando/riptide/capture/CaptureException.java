package org.zalando.riptide.capture;

import org.apiguardian.api.*;

import java.util.*;

import static org.apiguardian.api.API.Status.*;

@API(status = MAINTAINED)
public final class CaptureException extends NoSuchElementException {

    public CaptureException() {
        super("No value present");
    }

}
