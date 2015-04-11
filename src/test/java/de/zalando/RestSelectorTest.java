package de.zalando;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static de.zalando.RestDispatcher.contentType;
import static de.zalando.RestDispatcher.handle;
import static de.zalando.RestDispatcher.map;
import static de.zalando.RestDispatcher.on;
import static de.zalando.RestDispatcher.statusCode;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class RestSelectorTest {

    private final String url = "http://localhost/path";
    private final String textUrl = "http://localhost/path.txt";
    private final String jsonUrl = "http://localhost/path.json";

    private final RestTemplate template = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.createServer(template);

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectDuplicateAttributeValues() {
        on(template, contentType()).dispatch(
                handle(APPLICATION_JSON, Map.class, Object::toString),
                handle(APPLICATION_JSON, Map.class, Object::toString)
        );
    }
    
    @Test
    public void shouldConsumeTextPlain() {
        server.expect(requestTo(textUrl))
                .andRespond(withSuccess("It works!", TEXT_PLAIN));

        template.execute(textUrl, GET, null, on(template, contentType()).dispatch(
                handle(TEXT_PLAIN, String.class, Object::toString),
                handle(APPLICATION_JSON, Map.class, m -> {
                    throw new AssertionError("Didn't expect json");
                })
        ));
    }

    @Test
    public void shouldConsumeApplicationJson() {
        server.expect(requestTo(jsonUrl))
                .andRespond(withSuccess("{}", APPLICATION_JSON));

        template.execute(jsonUrl, GET, null, on(template, contentType()).dispatch(
                handle(TEXT_PLAIN, String.class, s -> {
                    throw new AssertionError("Didn't expect text");
                }),
                handle(APPLICATION_JSON, Map.class, Object::toString)
        ));
    }

    @Test
    public void shouldMapTextPlain() {
        server.expect(requestTo(textUrl))
                .andRespond(withSuccess("It works!", TEXT_PLAIN));

        final ResponseEntity<String> response = template.execute(textUrl, GET, null, on(template, contentType()).dispatch(
                map(TEXT_PLAIN, String.class, Object::toString),
                map(APPLICATION_JSON, Map.class, Object::toString)
        ));

        assertThat(response.getBody(), is("It works!"));
    }

    @Test
    public void shouldMapApplicationJson() {
        server.expect(requestTo(jsonUrl))
                .andRespond(withSuccess("{}", APPLICATION_JSON));

        final ResponseEntity<String> response = template.execute(jsonUrl, GET, null, on(template, contentType()).dispatch(
                map(TEXT_PLAIN, String.class, Object::toString),
                map(APPLICATION_JSON, Map.class, Object::toString)
        ));

        assertThat(response.getBody(), is("{}"));
    }

    @Test
    public void shouldConsumerOk() {
        server.expect(requestTo(url)).andRespond(withSuccess().body("It works!"));

        template.execute(url, GET, null, on(template, statusCode()).dispatch(
                handle(HttpStatus.OK, String.class, Object::toString),
                handle(HttpStatus.NOT_FOUND, String.class, s -> {
                    throw new AssertionError("Didn't expect 404");
                })
        ));
    }

    @Test
    public void shouldConsumeNotFound() {
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.NOT_FOUND).body("Not found"));

        template.setErrorHandler(new PassThroughResponseErrorHandler());
        template.execute(url, GET, null, on(template, statusCode()).dispatch(
                handle(HttpStatus.OK, String.class, s -> {
                    throw new AssertionError("Didn't expect 200");
                }),
                handle(HttpStatus.NOT_FOUND, String.class, Object::toString
                )
        ));
    }
    
    @Test(expected = RestClientException.class)
    public void shouldFailIfNoMatch() {
        server.expect(requestTo(jsonUrl))
                .andRespond(withSuccess("{}", APPLICATION_JSON));

        template.execute(jsonUrl, GET, null, on(template, contentType()).dispatch(
                handle(TEXT_PLAIN, String.class, Object::toString),
                handle(APPLICATION_XML, String.class, Object::toString)
        ));
    }

}