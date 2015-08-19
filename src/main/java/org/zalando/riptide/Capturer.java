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

import org.springframework.web.client.RestClientException;

@FunctionalInterface
public interface Capturer<A> {

    Binding<A> capture();

    /**
     *
     * @return
     * @throws IllegalStateException if the captured value is not an {@link Exception}
     */
    default Binding<A> propagate() {
        final Binding<A> binding = capture();
        return Binding.create(binding.getAttribute(), (response, converters) -> {
            final Object entity = binding.execute(response, converters).getValue();
            if (entity instanceof Exception) {
                throw (Exception) entity;
            } else {
                throw new IllegalStateException("unable to propagate non-throwable entity: " + entity);
            }
        });
    };

}
