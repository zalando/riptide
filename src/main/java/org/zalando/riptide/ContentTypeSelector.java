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

import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

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
final class ContentTypeSelector implements Selector<MediaType> {

    private static final Comparator<Binding<MediaType>> BY_SPECIFICITY =
            comparing(b -> b.getAttribute().get(), SPECIFICITY_COMPARATOR);

    @Override
    public Optional<MediaType> attributeOf(ClientHttpResponse response) throws IOException {
        return Optional.ofNullable(response.getHeaders().getContentType());
    }

    @Override
    public Optional<Binding<MediaType>> select(Optional<MediaType> attribute,
            Map<Optional<MediaType>, Binding<MediaType>> bindings) {

        return exactMatch(attribute, bindings)
                // needed because orElseGet unpacks the Optional, but we need one to return
                .map(Optional::of)
                .orElseGet(bestMatch(attribute, bindings));
    }

    private Optional<Binding<MediaType>> exactMatch(Optional<MediaType> attribute,
            Map<Optional<MediaType>, Binding<MediaType>> bindings) {
        return Selector.super.select(attribute, bindings);
    }

    private Supplier<Optional<Binding<MediaType>>> bestMatch(Optional<MediaType> attribute, Map<Optional<MediaType>, Binding<MediaType>> bindings) {
        return () -> attribute.flatMap(a -> bindings.values().stream()
                .filter(b -> b.getAttribute().isPresent())
                .sorted(BY_SPECIFICITY)
                .filter(b -> b.getAttribute().get().includes(a))
                .findFirst());
    }

}
