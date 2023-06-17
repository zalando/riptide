package org.zalando.riptide.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apiguardian.api.API;
import org.springframework.http.converter.HttpMessageConverter;

import java.util.List;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
@AllArgsConstructor
public final class ClientHttpMessageConverters {

    @Getter
    private final List<HttpMessageConverter<?>> converters;

}
