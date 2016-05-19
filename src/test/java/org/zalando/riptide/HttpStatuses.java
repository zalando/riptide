package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.springframework.http.HttpStatus;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.MOVED_TEMPORARILY;
import static org.springframework.http.HttpStatus.REQUEST_ENTITY_TOO_LARGE;
import static org.springframework.http.HttpStatus.REQUEST_URI_TOO_LONG;

final class HttpStatuses {

    @SuppressWarnings("deprecation")
    private static final ImmutableSet<HttpStatus> DEPRECATED = Sets.immutableEnumSet(
            MOVED_TEMPORARILY, // duplicate with FOUND
            REQUEST_ENTITY_TOO_LARGE, // duplicate with PAYLOAD_TOO_LARGE
            REQUEST_URI_TOO_LONG // duplicate with URI_TOO_LONG
    );

    static Stream<HttpStatus> supported() {
        final Predicate<HttpStatus> isDeprecated = DEPRECATED::contains;
        return Stream.of(HttpStatus.values()).filter(isDeprecated.negate());
    }

}
