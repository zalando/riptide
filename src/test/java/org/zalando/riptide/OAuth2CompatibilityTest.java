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
import org.springframework.security.oauth2.client.http.OAuth2ErrorHandler;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.status;

public class OAuth2CompatibilityTest {

    private final URI url = URI.create("http://localhost");
    private final RestTemplate template;

    public OAuth2CompatibilityTest() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());

        template = new RestTemplate();
        template.setMessageConverters(singletonList(converter));

        MockRestServiceServer server = MockRestServiceServer.createServer(template);
        server.expect(requestTo(url)).andRespond(withUnauthorizedRequest().body(new byte[] {0x13, 0x37}));
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void dispatchesConsumedResponseAgain() throws IOException {
        template.setErrorHandler(new OAuth2ErrorHandler(new OAuth2CompatibilityResponseErrorHandler(), null));
        Rest rest = Rest.create(template);
        
        ClientHttpResponse response = rest.execute(GET, url)
            .dispatch(status(), on(HttpStatus.UNAUTHORIZED).capture())
            .retrieveResponse()
            .orElseThrow(() -> new AssertionError("response expected"));
        
        assertThat(response.getBody().available(), is(2));
    }
    
    @Test
    public void responseIsConsumedIfOtherHandlerIsUsed() throws IOException {
        template.setErrorHandler(new OAuth2ErrorHandler(new PassThroughResponseErrorHandler(), null));
        Rest rest = Rest.create(template);
        
        ClientHttpResponse response = rest.execute(GET, url)
            .dispatch(status(), on(HttpStatus.UNAUTHORIZED).capture())
            .retrieveResponse()
            .orElseThrow(() -> new AssertionError("response expected"));
        
        // Since our mocked response is using a byte[] stream we check for the remaining bytes instead
        // of expecting an "already closed" IOException.
        assertThat(response.getBody().available(), is(0));
    }
}
