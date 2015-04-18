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

import com.google.common.reflect.TypeToken;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Binding.consume;
import static org.zalando.riptide.Binding.map;
import static org.zalando.riptide.RestDispatcher.contentType;
import static org.zalando.riptide.RestDispatcher.from;
import static org.zalando.riptide.RestDispatcher.statusCode;

public class RestDispatcherTest {

    private static final TypeToken<Map<String, Number>> MAP_TYPE =
            new TypeToken<Map<String, Number>>() {
            };

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final String url = "http://localhost/path";
    
    private final RestTemplate template = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.createServer(template);

    @SuppressWarnings("unchecked")
    private <T> Consumer<ResponseEntity<T>> consumer() {
        return mock(Consumer.class);
    }

    private ResponseEntity<String> anyText() {
        return Matchers.<ResponseEntity<String>>any();
    }

    private ResponseEntity<Map<String, Object>> anyJson() {
        return Matchers.<ResponseEntity<Map<String, Object>>>any();
    }
    
    @Test
    public void shouldRejectDuplicateAttributeValues() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("application/json"));
        exception.expectMessage(containsString("application/xml"));
        exception.expectMessage(not(containsString("text/plain")));

        from(template).dispatch(contentType(),
                consume(APPLICATION_JSON, Map.class, Object::toString),
                consume(APPLICATION_JSON, Map.class, Object::toString),
                consume(APPLICATION_XML, Map.class, Object::toString),
                consume(APPLICATION_XML, Map.class, Object::toString),
                consume(TEXT_PLAIN, String.class, Object::toString)
        );
    }

    @Test
    public void shouldConsumeTextPlain() {
        server.expect(requestTo(url))
                .andRespond(withSuccess("It works!", TEXT_PLAIN));

        final Consumer<ResponseEntity<String>> text = consumer();
        final Consumer<ResponseEntity<Map<String, Object>>> json = consumer();

        template.execute(url, GET, null, from(template).dispatch(contentType(),
                consume(TEXT_PLAIN, String.class, text),
                consume(APPLICATION_JSON, Map.class, json)
        ));

        verify(text).accept(anyText());
        verify(json, never()).accept(anyJson());
    }

    @Test
    public void shouldConsumeApplicationJson() {
        server.expect(requestTo(url))
                .andRespond(withSuccess("{}", APPLICATION_JSON));

        final Consumer<ResponseEntity<String>> text = consumer();
        final Consumer<ResponseEntity<Map<String, Object>>> json = consumer();

        template.execute(url, GET, null, from(template).dispatch(contentType(),
                consume(TEXT_PLAIN, String.class, text),
                consume(APPLICATION_JSON, Map.class, json)
        ));

        verify(text, never()).accept(anyText());
        verify(json).accept(anyJson());
    }

    @Test
    public void shouldMapTextPlain() {
        server.expect(requestTo(url))
                .andRespond(withSuccess("It works!", TEXT_PLAIN));

        final ResponseEntity<String> response = template.execute(url, GET, null, from(template).dispatch(contentType(),
                map(TEXT_PLAIN, String.class, HttpEntity::getBody),
                map(APPLICATION_JSON, Map.class, r -> "{}")
        ));

        assertThat(response.getBody(), is("It works!"));
    }

    @Test
    public void shouldMapApplicationJson() {
        server.expect(requestTo(url))
                .andRespond(withSuccess("{}", APPLICATION_JSON));

        final ResponseEntity<String> response = template.execute(url, GET, null, from(template).dispatch(contentType(),
                map(TEXT_PLAIN, String.class, r -> "Nope!"),
                map(APPLICATION_JSON, Map.class, r -> r.getBody().toString())
        ));

        assertThat(response.getBody(), is("{}"));
    }

    @Test
    public void shouldConsumeOk() {
        server.expect(requestTo(url)).andRespond(withSuccess().body("It works!"));

        final Consumer<ResponseEntity<String>> ok = consumer();
        final Consumer<ResponseEntity<String>> notFound = consumer();

        template.execute(url, GET, null, from(template).dispatch(statusCode(),
                consume(HttpStatus.OK, String.class, ok),
                consume(HttpStatus.NOT_FOUND, String.class, notFound)
        ));
        
        verify(ok).accept(anyText());
        verify(notFound, never()).accept(anyText());
    }

    @Test
    public void shouldConsumeNotFound() {
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.NOT_FOUND).body("Not found"));

        final Consumer<ResponseEntity<String>> ok = consumer();
        final Consumer<ResponseEntity<String>> notFound = consumer();
        
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        template.execute(url, GET, null, from(template).dispatch(statusCode(),
                consume(HttpStatus.OK, String.class, ok),
                consume(HttpStatus.NOT_FOUND, String.class, notFound)
        ));
        
        verify(ok, never()).accept(anyText());
        verify(notFound).accept(anyText());
    }

    @Test(expected = RestClientException.class)
    public void shouldFailIfNoMatch() {
        server.expect(requestTo(url))
                .andRespond(withSuccess("{}", APPLICATION_JSON));

        template.execute(url, GET, null, from(template).dispatch(contentType(),
                consume(TEXT_PLAIN, String.class, Object::toString),
                consume(APPLICATION_XML, String.class, Object::toString)
        ));
    }

    @Test
    public void shouldConsumeParameterizedType() {
        server.expect(requestTo(url))
                .andRespond(withSuccess("{\"value\":123}", APPLICATION_JSON));

        final Consumer<ResponseEntity<String>> text = consumer();
        final Consumer<ResponseEntity<Map<String, Number>>> json = consumer();

        template.execute(url, GET, null, from(template).dispatch(contentType(),
                consume(TEXT_PLAIN, String.class, text),
                consume(APPLICATION_JSON, MAP_TYPE, json)
        ));

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<ResponseEntity<Map<String, Number>>> captor =
                (ArgumentCaptor) ArgumentCaptor.forClass(ResponseEntity.class);
        
        verify(json).accept(captor.capture());
        
        assertThat(captor.getValue().getBody(), hasEntry("value", 123));
    }

    @Test
    public void shouldMapParameterizedType() {
        server.expect(requestTo(url))
                .andRespond(withSuccess("{\"value\":123}", APPLICATION_JSON));

        final ResponseEntity<Number> entity = template.execute(url, GET, null,
                from(template).dispatch(contentType(),
                        map(TEXT_PLAIN, String.class, response -> 0),
                        map(APPLICATION_JSON, MAP_TYPE, response ->
                                response.getBody().get("value"))
                ));

        assertThat(entity.getBody(), is(123));
    }

}