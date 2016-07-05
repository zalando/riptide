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

import com.google.common.reflect.TypeToken;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpMessageConverterExtractor;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

final class DefaultMessageReader implements MessageReader {

    private final List<HttpMessageConverter<?>> converters;

    DefaultMessageReader(final List<HttpMessageConverter<?>> converters) {
        this.converters = converters;
    }

    @Override
    public <I> I readEntity(final TypeToken<I> type, final ClientHttpResponse response) throws IOException {
        if (type.isSubtypeOf(ClientHttpResponse.class)) {
            return cast(response);
        }

        final I data = new HttpMessageConverterExtractor<I>(type.getType(), converters).extractData(response);
        if (!(data instanceof Closeable)) {
            response.close();
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private <I> I cast(ClientHttpResponse response) {
        return (I) response;
    }

}
