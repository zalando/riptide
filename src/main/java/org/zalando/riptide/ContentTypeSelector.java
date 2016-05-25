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

import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import static java.util.Comparator.comparing;
import static org.springframework.http.MediaType.SPECIFICITY_COMPARATOR;

/**
 * @see Selectors#contentType()
 */
enum ContentTypeSelector implements EqualitySelector<MediaType> {

    INSTANCE;

    private static final Comparator<Map.Entry<MediaType, Executor>> BY_SPECIFICITY =
            comparing(Map.Entry::getKey, SPECIFICITY_COMPARATOR);


    @Nullable
    @Override
    public MediaType attributeOf(ClientHttpResponse response) throws IOException {
        return response.getHeaders().getContentType();
    }

    @Override
    public Executor select(@Nullable final MediaType contentType,
            final Map<MediaType, Executor> routes) {

        if (contentType == null) {
            return null;
        }

        return Optional.ofNullable(exactMatch(contentType, routes))
                .orElse(bestMatch(contentType, routes));
    }

    @Nullable
    private Executor exactMatch(@Nullable final MediaType attribute,
            final Map<MediaType, Executor> routes) {
        return EqualitySelector.super.select(attribute, routes);
    }

    private Executor bestMatch(@Nullable final MediaType attribute,
            final Map<MediaType, Executor> routes) {
        
        Optional<Map.Entry<MediaType, Executor>> route = routes.entrySet().stream()
                .sorted(BY_SPECIFICITY)
                .filter(entry -> entry.getKey().includes(attribute))
                .findFirst();
        return route.isPresent() ? route.get().getValue() : null;
    }

}
