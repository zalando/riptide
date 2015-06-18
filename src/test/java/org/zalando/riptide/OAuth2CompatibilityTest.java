package org.zalando.riptide;

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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.OAuth2CompatibilityResponseErrorHandler;

import java.io.IOException;
import java.net.URI;

import static java.util.Collections.singletonList;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.status;

public class OAuth2CompatibilityTest {

    private final URI url = URI.create("http://localhost");
    private final RestTemplate template;
    private final MockRestServiceServer server;

    public OAuth2CompatibilityTest() {
        final OAuth2CompatibilityResponseErrorHandler handler = new OAuth2CompatibilityResponseErrorHandler();

        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());

        template = new RestTemplate();
        template.setMessageConverters(singletonList(converter));
        template.setErrorHandler(new OAuth2ResponseErrorHandler(handler));

        server = MockRestServiceServer.createServer(template);
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void dispatchesConsumedResponseAgain() throws IOException {
        server.expect(requestTo(url)).andRespond(withSuccess());

        Rest rest = Rest.create(template);
        rest.execute(GET, url).dispatch(status(), on(HttpStatus.OK).capture()).retrieveResponse()
            .orElseThrow(() -> new AssertionError("Expected a response"));
    }

    private static class OAuth2ResponseErrorHandler implements ResponseErrorHandler {

        private final ResponseErrorHandler innerHandler;

        private OAuth2ResponseErrorHandler(final ResponseErrorHandler innerHandler) {
            this.innerHandler = innerHandler;
        }

        @Override
        public boolean hasError(final ClientHttpResponse response) throws IOException {
            return true;
        }

        @Override
        public void handleError(final ClientHttpResponse response) throws IOException {
            // The actual OAuth2ResponseErrorHandler consumes the response here - we'll just pass it on
            innerHandler.handleError(response);
        }
    }
}
