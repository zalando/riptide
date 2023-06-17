package org.zalando.riptide.autoconfigure;

import org.apiguardian.api.API;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.function.Supplier;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@FunctionalInterface
public interface BaseURL extends Supplier<URI> {
    
    static BaseURL of(@Nullable final URI baseUrl) {
        return () -> baseUrl;
    }
    
}
