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

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.logging.Logger;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.riptide.Conditions.anyContentType;
import static org.zalando.riptide.Conditions.anySeries;
import static org.zalando.riptide.Conditions.anyStatusCode;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.MediaTypes.PROBLEM;
import static org.zalando.riptide.Selectors.contentType;
import static org.zalando.riptide.Selectors.series;
import static org.zalando.riptide.Selectors.statusCode;

public final class Usage {
    
    private static final Logger LOG = Logger.getLogger(Usage.class.getName());

    private final Rest rest = Rest.create(new RestTemplate());

    public void usage() {
        rest.execute(GET, URI.create("https://api.example.com")).dispatch(series(),
                on(SUCCESSFUL)
                        .dispatch(statusCode(),
                                on(CREATED, Success.class).call(this::onSuccess),
                                on(ACCEPTED, Success.class).call(this::onSuccess),
                                anyStatusCode().call(this::warn)),
                on(CLIENT_ERROR)
                        .dispatch(contentType(),
                                on(PROBLEM, Problem.class).call(this::onProblem),
                                on(APPLICATION_JSON, Problem.class).call(this::onProblem),
                                anyContentType().call(this::fail)),
                on(SERVER_ERROR).call(this::fail),
                anySeries().call(this::warn));
        
        // TODO what if I need the success element here now to return it?
    }

    private void onSuccess(ResponseEntity<Success> entity) {
        // TODO return something?
    }

    private void onProblem(Problem problem) {
        throw new ProblemException(problem);
    }

    private void warn(ClientHttpResponse response) {
        LOG.warning("Unexpected response: " + response);
    }

    private void fail(ClientHttpResponse response) {
        throw new AssertionError("Unexpected response: " + response);
    }

}