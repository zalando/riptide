package org.zalando.riptide.nakadi;

/*
 * ⁣​
 * Riptide: Nakadi
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */

import static org.zalando.riptide.stream.Streams.streamConverter;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.zalando.riptide.Rest;
import org.zalando.riptide.RestBuilder;
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.restdriver.clientdriver.ClientDriverRule;

public final class MockSetup {

    public static final int DEFAULT_MAX_CONNECTIONS = 2;

    private final RestBuilder builder;

    public MockSetup(final ClientDriverRule driver) {
        this(driver, defaultConverters(), defaultFactory());
    }

    public MockSetup(final ClientDriverRule driver, final Iterable<HttpMessageConverter<?>> converters,
            final AsyncClientHttpRequestFactory factory) {
        builder = Rest.builder()
                .converters(converters)
                .baseUrl(driver.getBaseUrl())
                .requestFactory(factory);
    }

    public static AsyncClientHttpRequestFactory defaultFactory() {
        return defaultFactory(defaultClient(defaultConfig(), DEFAULT_MAX_CONNECTIONS));
    }

    public static AsyncClientHttpRequestFactory defaultFactory(CloseableHttpClient client) {
        return new RestAsyncClientHttpRequestFactory(client,
                new ConcurrentTaskExecutor(Executors.newCachedThreadPool()));
    }

    public static CloseableHttpClient defaultClient(final RequestConfig config, final int connects) {
        return HttpClientBuilder.create()
                .setMaxConnPerRoute(connects)
                .setMaxConnTotal(connects)
                .setDefaultRequestConfig(config)
                .build();
    }

    public static RequestConfig defaultConfig() {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(0)
                .setConnectTimeout(0)
                .setSocketTimeout(0)
                .build();
    }

    public static List<HttpMessageConverter<?>> defaultConverters() {
        return defaultConverters(new ObjectMapper().findAndRegisterModules());
    }

    public static List<HttpMessageConverter<?>> defaultConverters(final ObjectMapper mapper) {
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return Arrays.asList(streamConverter(mapper),
                new MappingJackson2HttpMessageConverter(mapper));
    }

    public RestBuilder getRestBuilder() {
        return builder.clone();
    }

    public NakadiGateway getNakadi() {
        return new NakadiGateway(builder.build());
    }
}
