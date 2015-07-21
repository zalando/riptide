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

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;

public class OAuth2CompatibilityResponseErrorHandlerTest {

    private final OAuth2CompatibilityResponseErrorHandler unit = new OAuth2CompatibilityResponseErrorHandler();

    @Test
    public void isNoErrorForClientError() throws IOException {
        assertThat(unit.hasError(new MockClientHttpResponse(new byte[]{}, HttpStatus.BAD_REQUEST)), is(false));
    }

    @Test
    public void isNoErrorForServerError() throws IOException {
        assertThat(unit.hasError(new MockClientHttpResponse(new byte[]{}, HttpStatus.INTERNAL_SERVER_ERROR)),
                is(false));
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void throwsResponseWrappedInException() throws IOException {
        final ClientHttpResponse expectedResponse =
                new MockClientHttpResponse(new byte[]{0x13, 0x37}, HttpStatus.INTERNAL_SERVER_ERROR);

        exception.expect(AlreadyConsumedResponseException.class);
        exception.expect(hasFeature("response", AlreadyConsumedResponseException::getResponse, statusCodeMatcher()));

        unit.handleError(expectedResponse);
    }

    private Matcher<ClientHttpResponse> statusCodeMatcher() {
        return hasFeature("statusCode", new Function<ClientHttpResponse, HttpStatus>() {
            @Override
            public HttpStatus apply(ClientHttpResponse response) {
                try {
                    return response.getStatusCode();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, is(HttpStatus.INTERNAL_SERVER_ERROR));
    }

}
