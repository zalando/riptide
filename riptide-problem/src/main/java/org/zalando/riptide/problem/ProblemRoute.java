package org.zalando.riptide.problem;

import org.zalando.problem.Exceptional;
import org.zalando.riptide.Route;

import static org.springframework.http.MediaType.parseMediaType;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.contentType;
import static org.zalando.riptide.RoutingTree.dispatch;

public final class ProblemRoute {

    private static final Route ROUTE = dispatch(contentType(),
            on(parseMediaType("application/problem+json")).call(Exceptional.class, Exceptional::propagate),
            on(parseMediaType("application/x.problem+json")).call(Exceptional.class, Exceptional::propagate),
            on(parseMediaType("application/x-problem+json")).call(Exceptional.class, Exceptional::propagate));

    ProblemRoute() {
        // package private so we can trick code coverage
    }

    public static Route problemHandling() {
        return ROUTE;
    }

}
