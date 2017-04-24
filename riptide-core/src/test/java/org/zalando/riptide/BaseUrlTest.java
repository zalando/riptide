package org.zalando.riptide;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Route.pass;

public class BaseUrlTest {

    @Test
    public void shouldFailOnNullBaseURLQuery() {
        final MockSetup setup = new MockSetup(null);
        final Rest unit = setup.getRest();

        try {
            unit.get().dispatch(series(),
                    on(SUCCESSFUL).call(pass()));

            fail();
        } catch (NullPointerException ex) {
            assertThat(ex.getMessage(), is("base url required"));
        }
    }

}
