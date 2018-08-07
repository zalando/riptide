# Riptide: Client-side response routing

[![Tidal wave](docs/wave.jpg)](http://pixabay.com/en/wave-water-sea-tsunami-giant-wave-11061/)

[![Stability: Active](https://masterminds.github.io/stability/active.svg)](https://masterminds.github.io/stability/active.html)
[![Build Status](https://img.shields.io/travis/zalando/riptide/master.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide/master.svg)](https://coveralls.io/r/zalando/riptide)
[![Code Quality](https://img.shields.io/codacy/grade/1fbe3d16ca544c0c8589692632d114de/master.svg)](https://www.codacy.com/app/whiskeysierra/riptide)
[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-core.svg)](http://www.javadoc.io/doc/org.zalando/riptide-core)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-core.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-core)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

> **Riptide** noun, /ˈrɪp.taɪd/: strong flow of water away from the shore

*Riptide* is a library that implements ***client-side response routing***.  It tries to fill the gap between the HTTP
protocol and Java. Riptide allows users to leverage the power of HTTP with its unique API.

- **Technology stack**: Based on `spring-web` and uses the same foundation as Spring's RestTemplate.
- **Status**:  Actively maintained and used in production.
- Riptide is unique in the way that it doesn't abstract HTTP away, but rather embrace it!

## Example

Usage typically looks like this:

```java
http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
    .dispatch(series(),
        on(SUCCESSFUL).call(listOf(User.class), users -> 
            users.forEach(System.out::println)));
```

Feel free to compare this e.g. to [Feign](https://github.com/Netflix/feign#basics) or
[Retrofit](https://github.com/square/retrofit/blob/master/samples/src/main/java/com/example/retrofit/SimpleService.java).

## Features
- full access to the underlying HTTP client
- [resilience](docs/resilience.md) built into it
  - isolated thread pools, connection pools and bounded queues
  - transient fault detection via [riptide-faults](riptide-faults)
  - retries and circuit breaker via [Failsafe integration](riptide-failsafe)
  - backup requests via [riptide-backup](riptide-backup)
  - [timeouts](riptide-timeout)
- encourages the use of
  - fallbacks
  - content negotiation
  - robust error handling
- elegant syntax
- type-safe
- asynchronous by default
- [synchronous return values](riptide-capture) on demand
- [`application/problem+json` support](riptide-problem)
- [streaming](riptide-stream)

## Origin

Most modern clients try to adapt HTTP to a single-return paradigm as shown in the following example. Even though this
may be perfectly suitable for most applications it takes away a lot of the power that comes with HTTP. It's not easy to
support multiple different return values, i.e. distinct happy cases. Access to response headers or manual content
negotiation are also harder to do.
 
```java
@GET
@Path("/repos/{org}/{repo}/contributors")
List<User> getContributors(@PathParam String org, @PathParam String repo);
```
Riptide tries to counter this by providing a different approach to leverage the power of HTTP.
Go checkout the [concept document](docs/concepts.md) for more details.

## Dependencies

- Spring 4.x (4.3.0 or higher) **or 5.x**

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-core</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

Additional modules/artifacts of Riptide always share the same version number.

Alternatively, you can import our *bill of materials*...

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.zalando</groupId>
      <artifactId>riptide-bom</artifactId>
      <version>${riptide.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

... which allows you to omit versions:

```xml
<dependencies>
  <dependency>
      <groupId>org.zalando</groupId>
      <artifactId>riptide-core</artifactId>
  </dependency>
  <dependency>
      <groupId>org.zalando</groupId>
      <artifactId>riptide-failsafe</artifactId>
  </dependency>
  <dependency>
      <groupId>org.zalando</groupId>
      <artifactId>riptide-faults</artifactId>
  </dependency>
</dependencies>
```

## Configuration

Integration of your typical Spring Boot Application with Riptide, [Logbook](https://github.com/zalando/logbook) and
[Tracer](https://github.com/zalando/tracer) can be greatly simplified by using the
[**Riptide: Spring Boot Starter**](riptide-spring-boot-starter). Go check it out!

```java
Http.builder()
    .baseUrl("https://api.github.com")
    .requestFactory(new HttpComponentsAsyncClientHttpRequestFactory())
    .converter(new MappingJackson2HttpMessageConverter())
    .converter(new Jaxb2RootElementHttpMessageConverter())
    .plugin(new OriginalStackTracePlugin())
    .build();
```

The following code is the bare minimum, since a request factory is required:

```java
Http.builder()
    .requestFactory(new HttpComponentsAsyncClientHttpRequestFactory())
    .build();
```

This defaults to:
- no base URL
- same list of converters as `new RestTemplate()`
- [`OriginalStackTracePlugin`](#plugins)

## Usage

### Requests

A full-blown request may contain any of the following aspects: HTTP method, request URI, query parameters,
headers and a body:

```java
http.post("/sales-order")
    .queryParam("async", "false")
    .contentType(CART)
    .accept(SALES_ORDER)
    .header("Client-IP", "127.0.0.1")
    .body(cart)
    //...
```

Riptide supports the following HTTP methods: `get`, `head`, `post`, `put`, `patch`, `delete`, `options` and `trace`
respectively. Query parameters can either be provided individually using `queryParam(String, String)` or multiple at 
once with `queryParams(Multimap<String, String>)`.

The following operations are applied to URI Templates (`get(String, Object...)`) and URIs (`get(URI)`) respectively:

#### URI Template
- parameter expansion, e.g `/{id}` (see [`UriTemplate.expand`](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/util/UriTemplate.html#expand-java.lang.Object...-))
- encoding

#### URI
- none, used *as is*
- expected to be already encoded

#### Both
- after respective transformation
- resolved against Base URL (if present)
- Query String (merged with existing)
- Normalization

The [URI Resolution](docs/uri-resolution.md) table shows some examples how URIs are resolved against Base URLs, 
based on the chosen resolution strategy.

The `Content-Type`- and `Accept`-header have type-safe methods in addition to the generic support that is
`header(String, String)` and `headers(HttpHeaders)`.

### Responses

Riptide is special in the way it handles responses. Rather than having a single return value, you need to register
callbacks. Traditionally you would attach different callbacks for different response status codes, alternatively there
are also built-in routing capabilities on status code families (called series in Spring) as well as on content types. 

```java
http.post("/sales-order")
    // ...
    .dispatch(series(),
        on(SUCCESSFUL).dispatch(contentType(),
            on(SALES_ORDER).call(SalesOrder.class, this::persist),
        on(CLIENT_ERROR).dispatch(status(),
            on(CONFLICT).call(this::retry),
            on(PRECONDITION_FAILED).call(this::readAgainAndRetry),
            anyStatus().call(problemHandling())),
        on(SERVER_ERROR).dispatch(status(),
            on(SERVICE_UNAVAILABLE).call(this::scheduleRetryLater))));
```

The callbacks can have the following signatures:

```java
persist(SalesOrder)
retry(ClientHttpResponse)
scheduleRetryLater()
```

### Futures

Riptide will return a `CompletableFuture<Void>`. That means you can choose to chain transformations/callbacks or block
on it.

If you need proper return values take a look at [Riptide: Capture](riptide-capture).

### Exceptions

The only special custom exception you may get is `NoRouteException`, if and only if there was no matching condition and
no wildcard condition either.

### Plugins

Riptide comes with a way to register extensions in the form of plugins.

- `OriginalStackTracePlugin`, preserves stack traces when executing requests asynchronously
- [`FailsafePlugin`](riptide-failsafe), adds retries and circuit breaker support
- [`MetricsPlugin`](riptide-metrics), adds metrics for request duration
- [`TransientFaultPlugin`](riptide-faults), detects transient faults, e.g. network issues
- [`TimeoutPlugin`](riptide-timeout), applies timeouts to the whole call (including retries, network latency, etc.)

Whenever you encounter the need to perform some repetitive task on the futures returned by a remote call,
you may consider implementing a custom Plugin for it, e.g.:

```java
class MetricsPlugin implements Plugin {
    
    @Override
    public RequestExecution interceptBeforeRouting(RequestArguments arguments, RequestExecution execution) {
        return () -> {
           StopWatch watch = createStarted();
           CompletableFuture<ClientHttpResponse> future = execution.execute();
           future.whenComplete((result, e) -> {
               Duration duration = watch.elapsed();
               metrics.record(arguments, result, e, duration);
           });
           return future;
       };
    }
    
}

Http.builder()
    // ...
    .plugin(new MetricsPlugin(metrics))
    .build();
```

### Testing

Riptide is built on the same foundation as Spring's `RestTemplate` and `AsyncRestTemplate`. That allows us, with a small
trick, to use the same testing facilities, the [`MockRestServiceServer`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/client/MockRestServiceServer.html):

```java
AsyncRestTemplate template = new AsyncRestTemplate();
MockRestServiceServer server = MockRestServiceServer.createServer(template);
AsyncClientHttpRequestFactory requestFactory = template.getAsyncRequestFactory();

Http.builder()
    .requestFactory(requestFactory)
    // continue configuration
```

We basically use an intermediate `AsyncRestTemplate` as a holder of the special `AsyncClientHttpRequestFactory` that the
`MockRestServiceServer` manages.

If you are using the [Spring Boot Starter](riptide-spring-boot-starter) the test setup is provided by a convenient annotation `@RiptideClientTest`, 
see [here](riptide-spring-boot-starter/README.md#testing).

## Getting help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../issues).

## Getting involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change.
For more details check the [contribution guidelines](.github/CONTRIBUTING.md).

## Credits and references

- [URL routing](http://littledev.nl/?p=99)
