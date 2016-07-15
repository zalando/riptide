package org.zalando.riptide.stream;

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

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.stream.Streams.forEach;
import static org.zalando.riptide.stream.Streams.streamConverter;
import static org.zalando.riptide.stream.Streams.streamOf;

import org.springframework.http.MediaType;
import org.zalando.riptide.Rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

public final class SampleService {

    @JsonAutoDetect(fieldVisibility = NON_PRIVATE)
    static class User {
        String login;
        int contributions;
    }

    public static void main(final String... args) throws Exception {
        try (Rest rest = Rest.builder().baseUrl("https://api.github.com").converter(streamConverter()).build()) {
            rest.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                    .accept(MediaType.APPLICATION_JSON)
                    .dispatch(series(),
                            on(SUCCESSFUL).call(streamOf(User.class),
                                    forEach(user -> System.out.println(user.login + " (" + user.contributions + ")"))))
                    .get();
        }
    }
}
