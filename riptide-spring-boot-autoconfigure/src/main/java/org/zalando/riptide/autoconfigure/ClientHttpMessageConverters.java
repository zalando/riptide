package org.zalando.riptide.autoconfigure;

import org.apiguardian.api.*;
import org.springframework.http.converter.*;

import java.util.*;

import static org.apiguardian.api.API.Status.*;

@API(status = STABLE)
public final class ClientHttpMessageConverters {

    private final List<HttpMessageConverter<?>> converters;

    public ClientHttpMessageConverters(final List<HttpMessageConverter<?>> converters) {
        this.converters = converters;
    }

    public List<HttpMessageConverter<?>> getConverters() {
        return converters;
    }

}
