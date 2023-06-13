package org.zalando.riptide.autoconfigure;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.zalando.riptide.Http;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.zalando.riptide.autoconfigure.Name.name;

final class NameTest {

    @ParameterizedTest
    @CsvSource({
            "example,,exampleHttp",
            "my-example,,my-exampleHttp",
            ",,http",
            "example,Secure,exampleSecureHttp",
            "my-example,Secure,my-exampleSecureHttp",
            ",Secure,secureHttp"
    })
    void shouldToString(@Nullable final String id, @Nullable final String infix, final String expected) {
        assertEquals(expected, name(id, infix, Http.class).toString());
    }

    @ParameterizedTest
    @CsvSource({
            "example,,exampleHttp",
            "my-example,,myExampleHttp",
            ",,http",
            "example,Secure,exampleSecureHttp",
            "my-example,Secure,myExampleSecureHttp",
            ",Secure,secureHttp"
    })
    void shouldToNormalizedString(@Nullable final String id, @Nullable final String infix, final String expected) {
        assertEquals(expected, name(id, infix, Http.class).toNormalizedString());
    }

    @ParameterizedTest
    @CsvSource({
            "example,,exampleHttp,",
            "my-example,,my-exampleHttp,myExampleHttp",
            ",,http,",
            "example,Secure,exampleSecureHttp,",
            "my-example,Secure,my-exampleSecureHttp,myExampleSecureHttp",
            ",Secure,secureHttp,"
    })
    void shouldProduceAlternatives(@Nullable final String id, @Nullable final String infix,
            final String first, @Nullable final String second) {
        final Name unit = name(id, infix, Http.class);

        final Set<String> actual = unit.getAlternatives();

        assertEquals(Stream.of(first, second)
                .filter(Objects::nonNull).collect(toSet()), actual);
    }

}
