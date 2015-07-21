package org.example.application;

/*
 * ⁣​
 * riptide
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
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.OAuth2CompatibilityResponseErrorHandler;
import org.zalando.riptide.PassThroughResponseErrorHandler;
import org.zalando.riptide.Rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.zalando.riptide.Conditions.anyStatus;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.status;

/**
 * Simple sanity check to see if the API of riptide is actually public and accessible.
 */
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
    public void shouldNotConsumeMyResponse() throws IOException {
        setUp(new PassThroughResponseErrorHandler());

        server.expect(requestTo(url)).andRespond(this::onetimeConsumableResponse);

        final Map map = unit.execute(GET, url).dispatch(status(),
                on(OK, Map.class).capture(),
                anyStatus().call(this::error))
                .retrieve(Map.class).get();

        assertThat(map, is(emptyMap()));
    }

    @Test
    public void shouldNotConsumeMyResponseWithOAuth2CompatibilityHandler() throws IOException {
        setUp(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return true;
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                new OAuth2CompatibilityResponseErrorHandler().handleError(response);
            }
        });

        server.expect(requestTo(url)).andRespond(this::onetimeConsumableResponse);

        final Map map = unit.execute(GET, url).dispatch(status(),
                on(OK, Map.class).capture(),
                anyStatus().call(this::error))
                .retrieve(Map.class).get();

        assertThat(map, is(emptyMap()));
    }

    private void error(final ClientHttpResponse response) {
        throw new AssertionError("Should not have been called");
    }

    private ClientHttpResponse onetimeConsumableResponse(final ClientHttpRequest request) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(response.getStatusText()).thenReturn(HttpStatus.OK.getReasonPhrase());
        when(response.getHeaders()).thenReturn(headers);
        when(response.getBody()).thenReturn(new InputStream() {
            private boolean closed = false;

            private ByteArrayInputStream bytes = new ByteArrayInputStream("{}".getBytes());

            @Override
            public int read() throws IOException {
                if (closed) {
                    throw new IOException("Already closed");
                }
                return bytes.read();
            }

            @Override
            public void close() throws IOException {
                if (closed) {
                    throw new IOException("Already closed");
                }
                closed = true;
            }
        });
        doAnswer(invocationOnMock -> {
            ((ClientHttpResponse) invocationOnMock.getMock()).getBody().close();
            return null;
        }).when(response).close();
        return response;
    }

}
