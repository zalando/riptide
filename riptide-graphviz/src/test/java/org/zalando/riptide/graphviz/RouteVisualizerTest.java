package org.zalando.riptide.graphviz;

import org.junit.Test;
import org.zalando.riptide.Route;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.Route.pass;
import static org.zalando.riptide.RoutingTree.dispatch;
import static org.zalando.riptide.problem.ProblemRoute.problemHandling;

public class RouteVisualizerTest {

    @Test
    public void visualize() throws Exception {
        final Route route = dispatch(series(),
                on(SUCCESSFUL).call(pass()),
                on(CLIENT_ERROR).dispatch(status(),
                        on(CONFLICT).call(() -> {
                        })),
                anySeries().call(problemHandling()));


        final RouteVisualizer unit = RouteVisualizer.create(route);
        final String graph = unit.visualize();

        assertThat(graph, is("digraph G {\n" +
                "\n" +
                "  node [fontname=\"Arial\"];\n" +
                "  edge [fontname=\"Arial\"];\n" +
                "\n" +
                "  0 -> 1 [label=\"2xx\"]\n" +
                "  0 -> 2 [label=\"4xx\"]\n" +
                "  2 -> 3 [label=\"409\"]\n" +
                "  0 -> 4 [label=\"<any>\"]\n" +
                "  4 -> 5 [label=\"application/problem+json\\napplication/x.problem+json\\napplication/x-problem+json\"]\n" +
                "\n" +
                "  0 [label=\"Series\", shape=diamond]\n" +
                "  1 [label=\"Pass\", shape=box]\n" +
                "  2 [label=\"Status\", shape=diamond]\n" +
                "  3 [label=\"<code>\", shape=box]\n" +
                "  4 [label=\"Content Type\", shape=diamond]\n" +
                "  5 [label=\"Propagate\", shape=box]\n" +
                "\n" +
                "}"));
    }

}