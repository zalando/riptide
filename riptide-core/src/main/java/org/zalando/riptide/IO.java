package org.zalando.riptide;

import com.google.gag.annotation.remark.ThisWouldBeOneLineIn;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

interface IO extends RequestExecution {

    @ThisWouldBeOneLineIn(
            language = "Spring 5",
            toWit = "target.addAll(toMultiValueMap(source))")
    default void copyTo(
            final Map<String, List<String>> source,
            final MultiValueMap<String, String> target) {

        source.forEach((name, values) ->
                values.forEach(value ->
                        target.add(name, value)));
    }

}
