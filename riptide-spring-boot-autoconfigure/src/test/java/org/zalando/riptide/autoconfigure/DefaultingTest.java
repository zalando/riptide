package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.zalando.riptide.autoconfigure.RiptideProperties.Caching.Heuristic;

import java.nio.file.Paths;

import static java.util.concurrent.TimeUnit.HOURS;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.zalando.riptide.autoconfigure.RiptideProperties.Caching;

final class DefaultingTest {

    @Test
    void shouldMergeCaching() {
        final Caching actual = Defaulting.merge(
                new Caching(true, null, 4096, 100,
                        new Heuristic(0.1f, null)),
                new Caching(false, Paths.get("/var/cache/http"), 8192, 1000,
                        new Heuristic(0.25f, TimeSpan.of(1, HOURS))));

        assertThat(actual.getShared(), is(true));
        assertThat(actual.getDirectory(), hasToString("/var/cache/http"));
        assertThat(actual.getMaxObjectSize(), is(4096));
        assertThat(actual.getMaxCacheEntries(), is(100));
        assertThat(actual.getHeuristic().getCoefficient(), is(0.1f));
        assertThat(actual.getHeuristic().getDefaultLifeTime(), hasToString("1 hours"));
    }

}
