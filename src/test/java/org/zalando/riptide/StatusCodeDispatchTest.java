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
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.ALREADY_REPORTED;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.BANDWIDTH_LIMIT_EXCEEDED;
import static org.springframework.http.HttpStatus.CHECKPOINT;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CONTINUE;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.DESTINATION_LOCKED;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.FAILED_DEPENDENCY;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.FOUND;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.HTTP_VERSION_NOT_SUPPORTED;
import static org.springframework.http.HttpStatus.IM_USED;
import static org.springframework.http.HttpStatus.INSUFFICIENT_SPACE_ON_RESOURCE;
import static org.springframework.http.HttpStatus.INSUFFICIENT_STORAGE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT;
import static org.springframework.http.HttpStatus.LENGTH_REQUIRED;
import static org.springframework.http.HttpStatus.LOCKED;
import static org.springframework.http.HttpStatus.LOOP_DETECTED;
import static org.springframework.http.HttpStatus.METHOD_FAILURE;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.MOVED_PERMANENTLY;
import static org.springframework.http.HttpStatus.MOVED_TEMPORARILY;
import static org.springframework.http.HttpStatus.MULTIPLE_CHOICES;
import static org.springframework.http.HttpStatus.MULTI_STATUS;
import static org.springframework.http.HttpStatus.NETWORK_AUTHENTICATION_REQUIRED;
import static org.springframework.http.HttpStatus.NON_AUTHORITATIVE_INFORMATION;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NOT_EXTENDED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PARTIAL_CONTENT;
import static org.springframework.http.HttpStatus.PAYMENT_REQUIRED;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static org.springframework.http.HttpStatus.PRECONDITION_REQUIRED;
import static org.springframework.http.HttpStatus.PROCESSING;
import static org.springframework.http.HttpStatus.PROXY_AUTHENTICATION_REQUIRED;
import static org.springframework.http.HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
import static org.springframework.http.HttpStatus.REQUEST_ENTITY_TOO_LARGE;
import static org.springframework.http.HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;
import static org.springframework.http.HttpStatus.REQUEST_URI_TOO_LONG;
import static org.springframework.http.HttpStatus.RESET_CONTENT;
import static org.springframework.http.HttpStatus.RESUME_INCOMPLETE;
import static org.springframework.http.HttpStatus.SEE_OTHER;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.SWITCHING_PROTOCOLS;
import static org.springframework.http.HttpStatus.TEMPORARY_REDIRECT;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;
import static org.springframework.http.HttpStatus.UPGRADE_REQUIRED;
import static org.springframework.http.HttpStatus.USE_PROXY;
import static org.springframework.http.HttpStatus.VARIANT_ALSO_NEGOTIATES;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.zalando.riptide.Conditions.anyStatusCode;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.statusCode;


@RunWith(Parameterized.class)
public final class StatusCodeDispatchTest {

    private final URI url = URI.create("https://api.example.com");

    private final RestTemplate template = new RestTemplate();
    private final Rest unit = Rest.create(template);

    private final MockRestServiceServer server = MockRestServiceServer.createServer(template);

    private final HttpStatus status;

    public StatusCodeDispatchTest(HttpStatus status) {
        this.status = status;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return Stream.of(HttpStatus.values())
                .filter(s -> s != MOVED_TEMPORARILY) // duplicate with FOUND
                .map(s -> new Object[]{s})
                .collect(toList());
    }

    @Test
    public void shouldDispatch() {
        server.expect(requestTo(url)).andRespond(withStatus(status));

        @SuppressWarnings("unchecked")
        final Consumer<ClientHttpResponse> mock = mock(Consumer.class);

        unit.execute(GET, url)
                .dispatch(statusCode(),
                        on(CONTINUE).call(mock),
                        on(SWITCHING_PROTOCOLS).call(mock),
                        on(PROCESSING).call(mock),
                        on(CHECKPOINT).call(mock),
                        on(OK).call(mock),
                        on(CREATED).call(mock),
                        on(ACCEPTED).call(mock),
                        on(NON_AUTHORITATIVE_INFORMATION).call(mock),
                        on(NO_CONTENT).call(mock),
                        on(RESET_CONTENT).call(mock),
                        on(PARTIAL_CONTENT).call(mock),
                        on(MULTI_STATUS).call(mock),
                        on(ALREADY_REPORTED).call(mock),
                        on(IM_USED).call(mock),
                        on(MULTIPLE_CHOICES).call(mock),
                        on(MOVED_PERMANENTLY).call(mock),
                        on(FOUND).call(mock),
                        on(SEE_OTHER).call(mock),
                        on(NOT_MODIFIED).call(mock),
                        on(USE_PROXY).call(mock),
                        on(TEMPORARY_REDIRECT).call(mock),
                        on(RESUME_INCOMPLETE).call(mock),
                        on(BAD_REQUEST).call(mock),
                        on(UNAUTHORIZED).call(mock),
                        on(PAYMENT_REQUIRED).call(mock),
                        on(FORBIDDEN).call(mock),
                        on(NOT_FOUND).call(mock),
                        on(METHOD_NOT_ALLOWED).call(mock),
                        on(NOT_ACCEPTABLE).call(mock),
                        on(PROXY_AUTHENTICATION_REQUIRED).call(mock),
                        on(REQUEST_TIMEOUT).call(mock),
                        on(CONFLICT).call(mock),
                        on(GONE).call(mock),
                        on(LENGTH_REQUIRED).call(mock),
                        on(PRECONDITION_FAILED).call(mock),
                        on(REQUEST_ENTITY_TOO_LARGE).call(mock),
                        on(REQUEST_URI_TOO_LONG).call(mock),
                        on(UNSUPPORTED_MEDIA_TYPE).call(mock),
                        on(REQUESTED_RANGE_NOT_SATISFIABLE).call(mock),
                        on(EXPECTATION_FAILED).call(mock),
                        on(I_AM_A_TEAPOT).call(mock),
                        on(INSUFFICIENT_SPACE_ON_RESOURCE).call(mock),
                        on(METHOD_FAILURE).call(mock),
                        on(DESTINATION_LOCKED).call(mock),
                        on(UNPROCESSABLE_ENTITY).call(mock),
                        on(LOCKED).call(mock),
                        on(FAILED_DEPENDENCY).call(mock),
                        on(UPGRADE_REQUIRED).call(mock),
                        on(PRECONDITION_REQUIRED).call(mock),
                        on(TOO_MANY_REQUESTS).call(mock),
                        on(REQUEST_HEADER_FIELDS_TOO_LARGE).call(mock),
                        on(INTERNAL_SERVER_ERROR).call(mock),
                        on(NOT_IMPLEMENTED).call(mock),
                        on(BAD_GATEWAY).call(mock),
                        on(SERVICE_UNAVAILABLE).call(mock),
                        on(GATEWAY_TIMEOUT).call(mock),
                        on(HTTP_VERSION_NOT_SUPPORTED).call(mock),
                        on(VARIANT_ALSO_NEGOTIATES).call(mock),
                        on(INSUFFICIENT_STORAGE).call(mock),
                        on(LOOP_DETECTED).call(mock),
                        on(BANDWIDTH_LIMIT_EXCEEDED).call(mock),
                        on(NOT_EXTENDED).call(mock),
                        on(NETWORK_AUTHENTICATION_REQUIRED).call(mock),
                        anyStatusCode().call(this::fail));

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
