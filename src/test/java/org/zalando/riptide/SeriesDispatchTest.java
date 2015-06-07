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

import org.hamcrest.FeatureMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.INFORMATIONAL;
import static org.springframework.http.HttpStatus.Series.REDIRECTION;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Conditions.anySeries;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.series;

@RunWith(Parameterized.class)
public final class SeriesDispatchTest {
    
    private final URI url = URI.create("https://api.example.com");

    private final Rest unit;
    private final MockRestServiceServer server;

    private final HttpStatus status;

    public SeriesDispatchTest(HttpStatus status) {
        this.status = status;
        final RestTemplate template = new RestTemplate();
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {HttpStatus.CONTINUE},
                {HttpStatus.OK},
                {HttpStatus.MULTIPLE_CHOICES},
                {HttpStatus.BAD_REQUEST},
                {HttpStatus.INTERNAL_SERVER_ERROR}
        });
    }

    @Test
    public void shouldDispatch() {
        server.expect(requestTo(url)).andRespond(withStatus(status));

        @SuppressWarnings("unchecked")
        final Consumer<ClientHttpResponse> mock = mock(Consumer.class);
        
        unit.execute(GET, url)
                .dispatch(series(),
                        on(INFORMATIONAL).call(mock),
                        on(SUCCESSFUL).call(mock),
                        on(REDIRECTION).call(mock),
                        on(CLIENT_ERROR).call(mock),
                        on(SERVER_ERROR).call(mock),
                        anySeries().call(this::fail));
        
        verify(mock).accept(argThat(matches(status)));
    }

    private FeatureMatcher<ClientHttpResponse, HttpStatus> matches(final HttpStatus status) {
        return new FeatureMatcher<ClientHttpResponse, HttpStatus>(equalTo(status), "HTTP Status", "getStatusCode()") {
            @Override
            protected HttpStatus featureValueOf(ClientHttpResponse actual) {
                try {
                    return actual.getStatusCode();
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        };
    }

    private void fail(ClientHttpResponse response) {
        try {
            throw new AssertionError(response.getStatusCode().toString());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
