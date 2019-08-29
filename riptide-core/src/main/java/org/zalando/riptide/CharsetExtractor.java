package org.zalando.riptide;

import com.google.common.base.CharMatcher;
import com.google.gag.annotation.remark.OhNoYouDidnt;
import com.google.gag.annotation.remark.ThisWouldBeOneLineIn;
import org.apiguardian.api.API;
import org.springframework.http.MediaType;

import java.nio.charset.Charset;
import java.util.Optional;

import static org.apiguardian.api.API.Status.INTERNAL;

@API(status = INTERNAL)
public final class CharsetExtractor {

    private final CharMatcher quotes = CharMatcher.anyOf("\"'");

    @ThisWouldBeOneLineIn(
            language = "Spring 4.3+",
            toWit = "type.getCharset()")
    @OhNoYouDidnt
    public Optional<Charset> getCharset(final MediaType type) {
        return Optional.ofNullable(type.getParameter("charset"))
                .map(quotes::trimFrom)
                .map(Charset::forName);
    }

}
