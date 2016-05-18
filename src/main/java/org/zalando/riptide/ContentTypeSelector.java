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
import java.util.function.Supplier;

import static java.util.Comparator.comparing;
import static org.springframework.http.MediaType.SPECIFICITY_COMPARATOR;

/**
 * @see Selectors#contentType()
 */
enum ContentTypeSelector implements EqualitySelector<MediaType> {

    INSTANCE;

    private static final Comparator<Binding<MediaType>> BY_SPECIFICITY =
            comparing(Binding::getAttribute, SPECIFICITY_COMPARATOR);


    @Nullable
    @Override
    public MediaType attributeOf(ClientHttpResponse response) throws IOException {
        return response.getHeaders().getContentType();
    }

    @Override
    public Binding<MediaType> select(@Nullable final MediaType contentType,
            final Map<MediaType, Binding<MediaType>> bindings) {

        if (contentType == null) {
            return null;
        }

        return Optional.ofNullable(exactMatch(contentType, bindings))
                .orElseGet(bestMatch(contentType, bindings));
    }

    @Nullable
    private Binding<MediaType> exactMatch(@Nullable final MediaType attribute,
            final Map<MediaType, Binding<MediaType>> bindings) {
        return EqualitySelector.super.select(attribute, bindings);
    }

    private Supplier<Binding<MediaType>> bestMatch(@Nullable final MediaType attribute,
            final Map<MediaType, Binding<MediaType>> bindings) {

        return () -> bindings.values().stream()
                .sorted(BY_SPECIFICITY)
                .filter(binding -> binding.getAttribute().includes(attribute))
                .findFirst().orElse(null);
    }

}
