package org.zalando.riptide.micrometer.tag;

import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;

@AllArgsConstructor
abstract class ForwardingTagGenerator implements TagGenerator {

    @Delegate
    private final TagGenerator generator;

}
