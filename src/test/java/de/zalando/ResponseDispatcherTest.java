package de.zalando;

import com.google.common.reflect.TypeToken;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static de.zalando.ResponseDispatcher.on;
import static de.zalando.ResponseDispatcher.map;
import static de.zalando.ResponseDispatcher.handle;
import static org.springframework.http.HttpMethod.GET;

public class ResponseDispatcherTest {

    private final RestTemplate template = new RestTemplate();

    @Test
    public void shouldSupportConsumers() {
        template.execute("http://localhost/path", GET, null, on(template).dispatch(
                handle("text/plain", String.class, content -> {
                }),
                handle("application/json", new TypeToken<Map<String, Object>>() {
                }, map -> {
                })
        ));
    }
    
    @Test
    public void shouldSupportFunctions() {
        final String response = template.execute("http://localhost/path", GET, null, on(template).dispatch(
                map("text/plain", String.class, content -> content),
                map("application/json", new TypeToken<Map<String, Object>>() {
                }, m -> "" + m.size())
        ));
    }
    
}