package org.zalando.riptide;

import com.google.gag.annotation.remark.ThisWouldBeOneLineIn;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

import static org.springframework.util.CollectionUtils.toMultiValueMap;

interface IO extends RequestExecution {

    default void copyTo(
            final Map<String, List<String>> source,
            final MultiValueMap<String, String> target) {
        target.addAll(toMultiValueMap(source));
    }

}
