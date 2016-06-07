package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriTemplateHandler;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Capture.listOf;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.series;

public final class SampleService {

    @JsonAutoDetect(fieldVisibility = NON_PRIVATE)
    static class Contributor {
        String login;
        int contributions;
    }

    public static void main(final String... args) {
        final RestTemplate template = new RestTemplate();
        final DefaultUriTemplateHandler handler = new DefaultUriTemplateHandler();
        handler.setBaseUrl("https://api.github.com");
        template.setUriTemplateHandler(handler);
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        final Rest rest = Rest.create(template);

        rest.execute(GET, "/repos/zalando/riptide/contributors").dispatch(series(),
                on(SUCCESSFUL).call(listOf(Contributor.class), (List<Contributor> contributors) ->
                        contributors.forEach(contributor ->
                                System.out.println(contributor.login + " (" + contributor.contributions + ")"))));
    }

}
