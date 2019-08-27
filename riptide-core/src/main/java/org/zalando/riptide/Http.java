package org.zalando.riptide;

import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.*;
import org.springframework.web.client.*;

import javax.annotation.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.*;

import static org.apiguardian.api.API.Status.*;

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
public interface Http extends URIStage {

    static ExecutorStage builder() {
        return new DefaultHttpBuilder();
    }

    interface ExecutorStage {
        RequestFactoryStage executor(Executor executor);
        ConfigurationStage asyncRequestFactory(AsyncClientHttpRequestFactory asyncRequestFactory);
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
