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

import javax.annotation.Nullable;
import java.net.URI;
import java.util.logging.Logger;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;
import static org.zalando.riptide.Conditions.anyStatusCode;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.statusCode;

public final class CallCaptureUsage {

    private static final Logger LOG = Logger.getLogger(CallCaptureUsage.class.getName());

    private final Rest rest = Rest.create(new RestTemplate());

    @Nullable
    public Success usage() throws ProblemException {
        return rest.execute(POST, URI.create("https://api.example.com/accounts/123"))
                .dispatch(statusCode(),
                        on(OK, Account.class).call(this::extract).capture(),
                        anyStatusCode().call(this::warn))
                .retrieve(Success.class).orElse(null);
    }
    
    private Success extract(ResponseEntity<Account> entity) {
        final Account account = entity.getBody();
        final String eTag = entity.getHeaders().getETag();
        return new Success();
    }

    private void warn(ClientHttpResponse response) {
        LOG.warning("Unexpected response: " + response);
    }

}
