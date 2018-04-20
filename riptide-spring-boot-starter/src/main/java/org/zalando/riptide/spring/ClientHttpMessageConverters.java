package org.zalando.riptide.spring;

import org.apiguardian.api.API;
import org.springframework.http.converter.HttpMessageConverter;

import java.util.List;

import static org.apiguardian.api.API.Status.STABLE;

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
