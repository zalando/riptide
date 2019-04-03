package org.zalando.riptide.compatibility;

import com.google.gag.annotation.remark.Hack;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class UriVariables {

    private UriVariables() {

    }

    @Hack("Pretty dirty, but I couldn't find any other way...")
    static Object[] extract(final String template, final Map<String, ?> variables) {
        final List<Object> values = new ArrayList<>();

        UriComponentsBuilder.fromUriString(template).build().expand(name -> {
            if (!variables.containsKey(name)) {
                throw new IllegalArgumentException("Map has no value for '" + name + "'");
            }

            final Object value = variables.get(name);
            values.add(value);
            return value;
        });

        return values.toArray();
    }

}
