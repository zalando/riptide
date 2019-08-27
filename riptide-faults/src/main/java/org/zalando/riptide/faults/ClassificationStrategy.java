package org.zalando.riptide.faults;

import java.util.function.*;

public interface ClassificationStrategy {

    boolean test(Throwable throwable, Predicate<Throwable> predicate);

}
