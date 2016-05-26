package org.zalando.riptide;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriTemplateHandler;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Capture.listOf;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.series;

public final class SampleService {

    @JsonAutoDetect(fieldVisibility = NON_PRIVATE)
    static class Contributor {
        String login;
        int contributions;
    }

    public static void main(final String... args) {
        final RestTemplate template = new RestTemplate();
        final DefaultUriTemplateHandler handler = new DefaultUriTemplateHandler();
        handler.setBaseUrl("https://api.github.com");
        template.setUriTemplateHandler(handler);
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        final Rest rest = Rest.create(template);

        rest.execute(GET, "/repos/zalando/riptide/contributors").dispatch(series(),
                on(SUCCESSFUL).call(listOf(Contributor.class), (List<Contributor> contributors) ->
                        contributors.forEach(contributor ->
                                System.out.println(contributor.login + " (" + contributor.contributions + ")"))));
    }

}
