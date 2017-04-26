package org.zalando.riptide;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.http.HttpMethod;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Route.pass;

@RunWith(Parameterized.class)
public class NoUriTest {

    private final Rest unit;

    private final Executor executor;

    public NoUriTest(final Executor executor) {
        final MockSetup setup = new MockSetup(null);
        this.unit = setup.getRest();

        this.executor = executor;
    }

    interface Executor {
        Requester execute(Rest client);
    }

    @Parameterized.Parameters(name= "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {(Executor) Rest::get},
                {(Executor) Rest::head},
                {(Executor) Rest::post},
                {(Executor) Rest::put},
                {(Executor) Rest::patch},
                {(Executor) Rest::delete},
                {(Executor) Rest::options},
                {(Executor) Rest::trace},
        });
    }

    @Test
    public void shouldFailOnNullBaseURLQuery() {
        try {
            executor.execute(unit).dispatch(series(),
                    on(SUCCESSFUL).call(pass()));

            fail();
        } catch (final NullPointerException e) {
            assertThat(e.getMessage(), is("base url required"));
        }
    }

}
