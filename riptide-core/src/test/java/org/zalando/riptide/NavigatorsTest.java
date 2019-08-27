package org.zalando.riptide;

import com.google.common.reflect.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.HttpStatus.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.zalando.riptide.Navigators.*;

final class NavigatorsTest {

    @Test
    void shouldExposeTypeOfSeries() {
        assertThat(series().getType(), is(TypeToken.of(Series.class)));
    }

    @Test
    void shouldExposeTypeOfStatus() {
        assertThat(status().getType(), is(TypeToken.of(HttpStatus.class)));
    }

    @Test
    void shouldExposeTypeOfStatusCode() {
        assertThat(statusCode().getType(), is(TypeToken.of(Integer.class)));
    }

    @Test
    void shouldExposeTypeOfReasonPhrase() {
        assertThat(reasonPhrase().getType(), is(TypeToken.of(String.class)));
    }

    @Test
    void shouldExposeTypeOfContentType() {
        assertThat(contentType().getType(), is(TypeToken.of(MediaType.class)));
    }

}
