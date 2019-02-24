package org.zalando.riptide;

import com.google.common.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.zalando.riptide.Navigators.contentType;
import static org.zalando.riptide.Navigators.reasonPhrase;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.Navigators.statusCode;

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
