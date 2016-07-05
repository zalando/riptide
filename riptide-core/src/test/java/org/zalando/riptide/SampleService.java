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
import org.springframework.http.MediaType;

import java.io.IOException;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static java.lang.System.*;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PartialBinding.listOf;

public final class SampleService {

    @JsonAutoDetect(fieldVisibility = NON_PRIVATE)
    static class User {
        String login;
        int contributions;
    }

    public static void main(final String... args) throws IOException {
        try (Rest rest = Rest.create("https://api.github.com")) {
            rest.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                    .accept(MediaType.APPLICATION_JSON)
                    .dispatch(series(),
                            on(SUCCESSFUL).call(listOf(User.class), users ->
                                    users.forEach(user ->
                                            out.println(user.login + " (" + user.contributions + ")"))));
        }
    }

}
