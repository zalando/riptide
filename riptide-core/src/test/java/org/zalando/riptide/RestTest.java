package org.zalando.riptide;

import org.junit.Test;
import org.springframework.http.client.AsyncClientHttpRequestFactory;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@Deprecated
public class RestTest {

    @Test
    public void shouldCreateBuilder() {
        final Rest rest = Rest.builder()
                .requestFactory(mock(AsyncClientHttpRequestFactory.class))
                .build();

        assertThat(rest, is(notNullValue()));
    }

}
