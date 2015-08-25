package org.example.application;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 Zalando SE
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.EntityFunction;
import org.zalando.riptide.OAuth2CompatibilityResponseErrorHandler;
import org.zalando.riptide.PassThroughResponseErrorHandler;
import org.zalando.riptide.Rest;
import org.zalando.riptide.Retriever;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Conditions.anyStatus;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.status;


public class RestIntegrationTest {

    private final URI url = URI.create("http://localhost");

    private MockRestServiceServer server;
    private Rest unit;

    private void setUp(final ResponseErrorHandler errorHandler) {
        final RestTemplate template = new RestTemplate();
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        template.setMessageConverters(singletonList(converter));
        template.setErrorHandler(errorHandler);

        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Test
    public void shouldNotConsumeMyResponse() {
        setUp(new PassThroughResponseErrorHandler());

        server.expect(requestTo(url)).andRespond(r -> new OneTimeConsumableResponse("{}"));

        final Map map = unit.execute(GET, url).dispatch(status(),
                on(OK, Map.class).capture(),
                anyStatus().call(this::error))
                .retrieve(Map.class).get();

        assertThat(map, is(emptyMap()));
    }

    @Test
    public void shouldNotConsumeMyResponseWithOAuth2CompatibilityHandler() {
        setUp(new ResponseErrorHandler() {
            @Override
            public boolean hasError(final ClientHttpResponse response) {
                return true;
            }

            @Override
            public void handleError(final ClientHttpResponse response) throws IOException {
                new OAuth2CompatibilityResponseErrorHandler().handleError(response);
            }
        });

        server.expect(requestTo(url)).andRespond(r -> new OneTimeConsumableResponse("{}"));

        final Map map = unit.execute(GET, url).dispatch(status(),
                on(OK, Map.class).capture(),
                anyStatus().call(this::error))
                .retrieve(Map.class).get();

        assertThat(map, is(emptyMap()));
    }

    @Test
    public void shouldRetrieveParameterizedType() {
        setUp(new PassThroughResponseErrorHandler());

        server.expect(requestTo(url)).andRespond(withSuccess()
                .contentType(MediaType.APPLICATION_JSON)
                .body("[\"a\",\"b\"]"));

        final TypeToken<List<String>> typeToken = new TypeToken<List<String>>() {
        };
        final Optional<List<String>> resultOptional = unit.execute(GET, url).dispatch(status(),
                on(OK, typeToken).capture(),
                anyStatus().call(this::error))
                .retrieve(typeToken);

        assertThat(resultOptional, is(not(Optional.empty())));

        final List<String> result = resultOptional.get();
        assertThat(result, is(not(nullValue())));
        assertThat(result, hasSize(2));
        assertThat(result.get(0), isA(String.class));
    }

    @Test
    public void shouldRetrieveParameterizedTypeWithTypeInference() {
        setUp(new PassThroughResponseErrorHandler());

        server.expect(requestTo(url)).andRespond(withSuccess()
                .contentType(MediaType.APPLICATION_JSON)
                .body("[\"a\",\"b\"]"));

        final TypeToken<List<String>> typeToken = new TypeToken<List<String>>() {
        };
        final List<String> result = unit.execute(GET, url).dispatch(status(),
                on(OK, typeToken).capture(),
                anyStatus().call(this::error))
                .retrieve(typeToken).orElseThrow(() -> new RuntimeException("Unable to retrieve List<String>"));

        assertThat(result, is(not(nullValue())));
        assertThat(result, hasSize(2));
        assertThat(result.get(0), isA(String.class));
    }

    @Test
    public void shouldNotRetrieveMappedParameterizedType() {
        setUp(new PassThroughResponseErrorHandler());

        server.expect(requestTo(url)).andRespond(withSuccess()
                .contentType(MediaType.APPLICATION_JSON)
                .body("[\"a\",\"b\"]"));

        final TypeToken<List<String>> typeToken = new TypeToken<List<String>>() {
        };
        final Retriever retriever = unit.execute(GET, url).dispatch(status(),
                on(OK, typeToken).map((EntityFunction<List<String>, Object, Exception>) strings -> strings).capture(),
                anyStatus().call(this::error));

        assertThat(retriever.retrieve(typeToken), is(Optional.empty()));
        assertThat(retriever.retrieve(List.class), is(not(Optional.empty())));
    }

    private void error(final ClientHttpResponse response) {
        throw new AssertionError("Should not have been called");
    }
}
