package org.zalando.riptide.graphviz;

import org.junit.Test;
import org.zalando.riptide.graphviz.RouteVisualizer.Edge;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

public final class EdgeTest {

    @Test
    public void shouldBeReflexive() {
        final Edge edge = new Edge(0, 1);
        assertThat(edge, is(equalTo(edge)));
    }

    @Test
    public void shouldBeEquals() {
        assertThat(new Edge(0, 1), is(equalTo(new Edge(0, 1))));
    }

    @Test
    public void shouldNotBeEquals() {
        assertThat(new Edge(0, 1), is(not(equalTo(new Edge(1, 1)))));
        assertThat(new Edge(0, 1), is(not(equalTo(new Edge(0, 0)))));
        assertThat(new Edge(0, 1), is(not(equalTo(new Edge(1, 0)))));
    }

    @Test
    public void shouldNotBeEqualToDifferentType() {
        assertThat(new Edge(0, 1), is(not(equalTo("foo"))));
    }

}