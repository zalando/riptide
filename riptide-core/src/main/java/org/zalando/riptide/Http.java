package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static org.apiguardian.api.API.Status.STABLE;

/**
 * Central class for actual asynchronous HTTP-based communication. Http is loosely modeled after the HTTP protocol,
 * i.e. you start with a method and by a URL and optionally followed query parameters, headers and a body:
 *
 * <pre>{@code http.get("/users")
 *     .queryParam("active", "true")
 *     .accept(APPLICATION_JSON)
 *     .dispatch(..)}</pre>
 *
 * @see RestTemplate
 */
@API(status = STABLE)
public interface Http {

    Requester get(String uriTemplate, Object... urlVariables);
    Requester get(URI uri);
    Requester get();

    Requester head(String uriTemplate, Object... urlVariables);
    Requester head(URI uri);
    Requester head();

    Requester post(String uriTemplate, Object... urlVariables);
    Requester post(URI uri);
    Requester post();

    Requester put(String uriTemplate, Object... urlVariables);
    Requester put(URI uri);
    Requester put();

    Requester patch(String uriTemplate, Object... urlVariables);
    Requester patch(URI uri);
    Requester patch();

    Requester delete(String uriTemplate, Object... urlVariables);
    Requester delete(URI uri);
    Requester delete();

    Requester options(String uriTemplate, Object... urlVariables);
    Requester options(URI uri);
    Requester options();

    Requester trace(String uriTemplate, Object... urlVariables);
    Requester trace(URI uri);
    Requester trace();

    Requester execute(HttpMethod method, String uriTemplate, Object... uriVariables);
    Requester execute(HttpMethod method, URI uri);
    Requester execute(HttpMethod method);

    static ExecutorStage builder() {
        return new DefaultHttpBuilder();
    }

    interface ExecutorStage {
        RequestFactoryStage executor(Executor executor);
    }

    interface RequestFactoryStage {
        ConfigurationStage requestFactory(ClientHttpRequestFactory requestFactory);
    }

    interface ConfigurationStage extends FinalStage {
        ConfigurationStage defaultConverters();
        ConfigurationStage converters(Iterable<HttpMessageConverter<?>> converters);
        ConfigurationStage converter(HttpMessageConverter<?> converter);
        ConfigurationStage baseUrl(@Nullable String baseUrl);
        ConfigurationStage baseUrl(@Nullable URI baseUrl);
        ConfigurationStage baseUrl(Supplier<URI> baseUrlProvider);
        ConfigurationStage urlResolution(@Nullable UrlResolution resolution);
        ConfigurationStage defaultPlugins();
        ConfigurationStage plugins(Iterable<Plugin> plugins);
        ConfigurationStage plugin(Plugin plugin);
    }

    interface FinalStage {
        Http build();
    }

}
