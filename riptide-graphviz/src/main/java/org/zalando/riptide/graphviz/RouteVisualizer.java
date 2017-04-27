package org.zalando.riptide.graphviz;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.zalando.riptide.Navigator;
import org.zalando.riptide.Route;
import org.zalando.riptide.RoutingTree;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.joining;

public final class RouteVisualizer {

    @VisibleForTesting
    static final class Edge {
        private final Integer source;
        private final Integer target;

        Edge(final Integer source, final Integer target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public boolean equals(final Object that) {
            if (this == that) {
                return true;
            } else if (that instanceof Edge) {
                final Edge other = (Edge) that;
                return Objects.equals(source, other.source) &&
                        Objects.equals(target, other.target);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, target);
        }

        public Integer getSource() {
            return source;
        }

        public Integer getTarget() {
            return target;
        }
    }

    private final BiMap<Route, Integer> nodes = HashBiMap.create();
    private final Multimap<Edge, String> edges = LinkedHashMultimap.create();
    private final Route root;

    private RouteVisualizer(final Route root) {
        this.root = root;
    }

    String visualize() {
        walk(root);

        final StringBuilder output = new StringBuilder();

        output.append("digraph G {\n\n");

        output.append("  node [fontname=\"Arial\"];\n");
        output.append("  edge [fontname=\"Arial\"];\n\n");

        edges.keySet().forEach(edge ->
                output
                        .append("  ")
                        .append(edge.getSource())
                        .append(" -> ")
                        .append(edge.getTarget())
                        .append(" [label=\"")
                        .append(edges.get(edge).stream().collect(joining("\\n")))
                        .append("\"]\n"));
        output.append("\n");

        nodes.forEach((route, id) ->
                output
                        .append("  ")
                        .append(id)
                        .append(" [label=\"")
                        .append(route.toString())
                        .append("\", shape=")
                        .append(route instanceof RoutingTree ? "diamond" : "box")
                        .append("]\n"));
        output.append("\n");

        output.append("}");

        return output.toString();
    }

    private void walk(final Route route) {
        if (route instanceof RoutingTree) {
            final Integer id = identify(route);
            @SuppressWarnings("unchecked") final RoutingTree<Object> tree = (RoutingTree) route;

            final Navigator<Object> navigator = tree.getNavigator();
            final Set<Object> attributes = tree.keySet();

            attributes.forEach(attribute -> {
                final Route child = tree.get(attribute).orElseThrow(NoSuchElementException::new);
                descend(id, child, navigator.toString(attribute));
            });

            tree.getWildcard().ifPresent(child ->
                    descend(id, child, "<any>"));
        } else {
            identify(route);
        }
    }

    private void descend(final Integer id, final Route child, final String value) {
        edges.put(new Edge(id, identify(child)), value);
        walk(child);
    }

    private Integer identify(final Route route) {
        return nodes.computeIfAbsent(route, ignored -> nodes.size());
    }

    public static RouteVisualizer create(final Route root) {
        return new RouteVisualizer(root);
    }

}
