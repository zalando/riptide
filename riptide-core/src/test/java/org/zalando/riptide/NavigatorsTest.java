package org.zalando.riptide;

import com.google.common.reflect.TypeToken;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;

import java.util.Arrays;

import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.Series.INFORMATIONAL;
import static org.zalando.riptide.Navigators.contentType;
import static org.zalando.riptide.Navigators.reasonPhrase;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.Navigators.statusCode;

@RunWith(Parameterized.class)
public final class NavigatorsTest<T> {

    private final Navigator<T> navigator;
    private final Class<T> type;
    private final String name;
    private final T attribute;
    private final String attributeName;

    public NavigatorsTest(final Navigator<T> navigator, final Class<T> type, final String name, final T attribute,
            final String attributeName) {
        this.navigator = navigator;
        this.type = type;
        this.name = name;
        this.attribute = attribute;
        this.attributeName = attributeName;
    }

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {series(), Series.class, "Series", INFORMATIONAL, "1xx"},
                {status(), HttpStatus.class, "Status", OK, "200"},
                {statusCode(), Integer.class, "Status Code", 200, "200"},
                {reasonPhrase(), String.class, "Reason Phrase", "OK", "OK"},
                {contentType(), MediaType.class, "Content Type", MediaType.TEXT_PLAIN, "text/plain"},
        });
    }

    @Test
    public void shouldExposeType() {
        assertThat(navigator.getType(), is(TypeToken.of(type)));
    }

    @Test
    public void shouldHaveName() {
        assertThat(navigator, hasToString(name));
    }

    @Test
    public void shouldNameAttribute() {
        assertThat(navigator.toString(attribute), is(attributeName));
    }

}