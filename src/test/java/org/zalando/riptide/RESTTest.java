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

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.function.Consumer;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.INFORMATIONAL;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.riptide.Binding.any;
import static org.zalando.riptide.Binding.on;
import static org.zalando.riptide.MediaTypes.PROBLEM;
import static org.zalando.riptide.Selectors.contentType;
import static org.zalando.riptide.Selectors.series;
import static org.zalando.riptide.Selectors.statusCode;

public final class RestTest {

    private final Rest rest = Rest.create(new RestTemplate());

    @Test
    public void usage() {
        final ResponseConsumer<Problem> onProblem = this::onProblem;
        rest.execute(GET, URI.create("https://api.example.com")).dispatch(series(),
                on(INFORMATIONAL)
                        .dispatch(statusCode(),
                                on(CREATED, Success.class).call(this::onSuccess),
                                on(ACCEPTED, Success.class).call(this::onSuccess),
                                Binding.<HttpStatus>any().call(this::warn)),
                on(CLIENT_ERROR)
                        .dispatch(contentType(),
                                on(PROBLEM, Problem.class).call(onProblem),
                                on(APPLICATION_JSON, Problem.class).call(onProblem)),
                on(SERVER_ERROR).call(this::fail),
                Binding.<HttpStatus.Series>any().call(this::fail));
    }

    private void onSuccess(Success entity) {

    }

    private void onProblem(ResponseEntity<Problem> entity) {
    }

    private void warn(ClientHttpResponse response) {

    }

    private void fail(ClientHttpResponse response) {
        throw new AssertionError("Didn't expect this: " + response);
    }

}