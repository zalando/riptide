package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.zalando.riptide.autoconfigure.RiptideProperties.Caching.Heuristic;
import org.zalando.riptide.autoconfigure.RiptideProperties.Client;
import org.zalando.riptide.autoconfigure.RiptideProperties.Connections;
import org.zalando.riptide.autoconfigure.RiptideProperties.Defaults;
import org.zalando.riptide.autoconfigure.RiptideProperties.Threads;

import java.nio.file.Paths;

import static java.util.concurrent.TimeUnit.HOURS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.zalando.riptide.autoconfigure.RiptideProperties.Caching;

/**
 * defaults connections max total = max(defaults connections max per route, ...)
 * defaults thread max size = coalesce(..., defaults connections max total)
 * clients connections max per route = coalesce(..., defaults connections max per route)
 * clients connections max total = coalesce(..., defaults connections max total)
 * clients connections max total = max(defaults connections max per route, ...)
 * clients connections max total = coalesce(..., max(defaults connections
 */
final class DefaultingTest {

    @Test
    void shouldRaiseProvidedDefaultConnectionsMaxTotalToConnectionsMaxPerRoute() {
        final RiptideProperties properties = new RiptideProperties();
        properties.getDefaults().getConnections().setMaxPerRoute(50);
        properties.getDefaults().getConnections().setMaxTotal(25);
        final RiptideProperties actual = Defaulting.withDefaults(properties);

        assertThat(actual.getDefaults().getConnections().getMaxTotal(), is(50));
    }

    @Test
    void shouldNotLowerProvidedDefaultConnectionMaxTotalToConnectionsMaxPerRoute() {
        final RiptideProperties properties = new RiptideProperties();
        properties.getDefaults().getConnections().setMaxTotal(75);
        final RiptideProperties actual = Defaulting.withDefaults(properties);

        assertThat(actual.getDefaults().getConnections().getMaxTotal(), is(75));
    }

    @Test
    void shouldOverrideUnprovidedDefaultThreadsMaxSizeToConnectionsMaxTotal() {
        final RiptideProperties properties = new RiptideProperties();
        properties.getDefaults().getConnections().setMaxTotal(50);
        final RiptideProperties actual = Defaulting.withDefaults(properties);

        assertThat(actual.getDefaults().getThreads().getMaxSize(), is(50));
    }

    @Test
    void shouldNotOverwriteProvidedDefaultThreadsMaxSizeWithConnectionsMaxTotal() {
        final RiptideProperties properties = new RiptideProperties();
        final Defaults defaults = new Defaults();
        defaults.setThreads(new Threads(true, null, 10, null, null));
        properties.setDefaults(defaults);
        final RiptideProperties actual = Defaulting.withDefaults(properties);

        assertThat(actual.getDefaults().getThreads().getMaxSize(), is(10));
    }

    @Test
    void shouldNotRaiseProvidedClientConnectionsMaxTotalToDefaultConnectionsMaxTotal() {
        final RiptideProperties properties = new RiptideProperties();
        properties.getDefaults().getConnections().setMaxTotal(100);
        final Connections connections = new Connections();
        connections.setMaxTotal(50);
        final Client client = new Client();
        client.setConnections(connections);
        properties.getClients().put("example", client);
        final RiptideProperties actual = Defaulting.withDefaults(properties);

        assertThat(actual.getClients().get("example").getConnections().getMaxTotal(), is(50));
    }

    @Test
    void shouldNotLowerProvidedClientConnectionsMaxTotalToDefaultConnectionsMaxTotal() {
        final RiptideProperties properties = new RiptideProperties();
        properties.getDefaults().getConnections().setMaxTotal(50);
        final Connections connections = new Connections();
        connections.setMaxTotal(100);
        final Client client = new Client();
        client.setConnections(connections);
        properties.getClients().put("example", client);
        final RiptideProperties actual = Defaulting.withDefaults(properties);

        assertThat(actual.getClients().get("example").getConnections().getMaxTotal(), is(100));
    }

    @Test
    void shouldOverwriteUnprovidedClientConnectionsMaxTotalToDefaultConnectionsMaxTotal() {
        final RiptideProperties properties = new RiptideProperties();
        properties.getDefaults().getConnections().setMaxTotal(50);
        properties.getClients().put("example", new Client());
        final RiptideProperties actual = Defaulting.withDefaults(properties);

        assertThat(actual.getClients().get("example").getConnections().getMaxTotal(), is(50));
    }

    @Test
    void shouldRaiseThreadsMaxSizeToConnectionsMaxTotalWhenLower() {
        // When explicit threads.max-size is lower than effective connections.max-total,
        // thread pool is raised to max-total to avoid throughput bottleneck
        final RiptideProperties properties = new RiptideProperties();
        properties.getDefaults().getThreads().setMaxSize(15);
        properties.getDefaults().getConnections().setMaxPerRoute(30);
        properties.getClients().put("example", new Client());
        final RiptideProperties actual = Defaulting.withDefaults(properties);

        assertThat(actual.getClients().get("example").getThreads().getMaxSize(), is(30));
    }

    @Test
    void shouldFallbackClientThreadsMaxSizeToConnectionsMaxTotalWhenNotConfigured() {
        // Auto-sizing fallback: when threads.max-size is NOT configured anywhere,
        // the client's thread pool should auto-size to connections.max-per-route
        final RiptideProperties properties = new RiptideProperties();
        properties.getDefaults().getConnections().setMaxPerRoute(30);
        properties.getClients().put("example", new Client());
        final RiptideProperties actual = Defaulting.withDefaults(properties);

        assertThat(actual.getClients().get("example").getThreads().getMaxSize(), is(30));
    }

    @Test
    void shouldRaiseClientThreadsMaxSizeToConnectionsMaxTotalWhenLower() {
        // Client-level explicit threads.max-size is raised to connections.max-total if lower
        final RiptideProperties properties = new RiptideProperties();
        properties.getDefaults().getThreads().setMaxSize(15);
        properties.getDefaults().getConnections().setMaxPerRoute(30);
        final Client client = new Client();
        client.setThreads(new Threads(true, null, 5, null, null));
        properties.getClients().put("example", client);
        final RiptideProperties actual = Defaulting.withDefaults(properties);

        assertThat(actual.getClients().get("example").getThreads().getMaxSize(), is(30));
    }

    @Test
    void shouldFallbackClientThreadsMaxSizeToClientConnectionsMaxTotalWhenNotConfigured() {
        // When no threads.max-size is configured anywhere, client-specific connections
        // should drive the thread pool size (not global defaults connections)
        final RiptideProperties properties = new RiptideProperties();
        // no threads.max-size configured anywhere
        final Client client = new Client();
        client.setConnections(new Connections());
        client.getConnections().setMaxPerRoute(50); // client has its own higher connection count
        properties.getClients().put("example", client);
        final RiptideProperties actual = Defaulting.withDefaults(properties);

        assertThat(actual.getClients().get("example").getThreads().getMaxSize(), is(50));
    }

    @Test
    void shouldMergeCaching() {
        final Caching actual = Defaulting.merge(
                new Caching(false, true, null, 4096, 100,
                        new Heuristic(Boolean.FALSE, 0.1f, null)),
                new Caching(true, false, Paths.get("/var/cache/http"), 8192, 1000,
                        new Heuristic(true, 0.25f, TimeSpan.of(1, HOURS))));

        assertThat(actual.getEnabled(), is(false));
        assertThat(actual.getShared(), is(true));
        assertThat(actual.getDirectory(), is(Paths.get("/var/cache/http")));
        assertThat(actual.getMaxObjectSize(), is(4096));
        assertThat(actual.getMaxCacheEntries(), is(100));
        assertThat(actual.getHeuristic().getEnabled(), is(false));
        assertThat(actual.getHeuristic().getCoefficient(), is(0.1f));
        assertThat(actual.getHeuristic().getDefaultLifeTime(), hasToString("1 hours"));
    }

}
