# Riptide: A next generation HTTP client 

[![Tidal wave](docs/wave.jpg)](http://pixabay.com/en/wave-water-sea-tsunami-giant-wave-11061/)

[![Stability: Active](https://masterminds.github.io/stability/active.svg)](https://masterminds.github.io/stability/active.html)
![Build Status](https://github.com/zalando/riptide/workflows/build/badge.svg)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide/main.svg)](https://coveralls.io/r/zalando/riptide)
[![Code Quality](https://img.shields.io/codacy/grade/1fbe3d16ca544c0c8589692632d114de/main.svg)](https://www.codacy.com/app/whiskeysierra/riptide)
[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-core.svg)](http://www.javadoc.io/doc/org.zalando/riptide-core)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-core.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-core)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/main/LICENSE)
[![Project Dashboard](https://sourcespy.com/shield.svg)](https://sourcespy.com/github/zalandoriptide/)

> **Riptide** noun, /ˈrɪp.taɪd/: strong flow of water away from the shore

*Riptide* is a library that implements ***client-side response routing***.  It tries to fill the gap between the HTTP
protocol and Java. Riptide allows users to leverage the power of HTTP with its unique API.

- **Technology stack**: Based on `spring-web` and uses the same foundation as Spring's RestTemplate.
- **Status**:  Actively maintained and used in production.
- Riptide is unique in the way that it doesn't abstract HTTP away, but rather embraces it!

:rotating_light: **Upgrading from 2.x to 3.x?** Please refer to the [Migration Guide](MIGRATION.md).

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
  - retries, circuit breaker, backup requests and timeouts via [Failsafe integration](riptide-failsafe)
- non-blocking IO (optional)
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
may be perfectly suitable for most applications, it takes away a lot of the power that comes with HTTP. It's not easy to
support multiple return values, i.e. distinct happy cases. Access to response headers or manual content
negotiation are also more difficult.
 
```java
@GET
@Path("/repos/{org}/{repo}/contributors")
List<User> getContributors(@PathParam String org, @PathParam String repo);
```
Riptide tries to counter this by providing a different approach to leverage the power of HTTP.
Go checkout the [concept document](docs/concepts.md) for more details.

## Dependencies

- Spring 4.1 or higher
  - :warning: Spring Boot integration requires Spring 5

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
    .executor(Executors.newCachedThreadPool())
    .requestFactory(new HttpComponentsClientHttpRequestFactory())
    .baseUrl("https://api.github.com")
    .converter(new MappingJackson2HttpMessageConverter())
    .converter(new Jaxb2RootElementHttpMessageConverter())
    .plugin(new OriginalStackTracePlugin())
    .build();
```

The following code is the bare minimum, since a request factory is required:

```java
Http.builder()
    .executor(Executors.newCachedThreadPool())
    .requestFactory(new HttpComponentsClientHttpRequestFactory())
    .build();
```

This defaults to:
- no base URL
- same list of converters as `new RestTemplate()`
- [`OriginalStackTracePlugin`](#plugins)

### Thread Pool

All off the standard `Executors.new*Pool()` implementations only support the *queue-first* style, i.e. the pool scales up to the core pool size, then fills the queue and only then will scale up to the maximum pool size.

Riptide provides a `ThreadPoolExecutors.builder()` which also offers a *scale-first* style where thread pools scale up to the maximum pool size before they queue any tasks. That usually leads to higher throughput, lower latency on the expense of having to maintain more threads. 

The following table shows which combination of properties are supported

| Configuration                              | Supported           |
|--------------------------------------------|---------------------|
| Without queue, fixed size¹                 | :heavy_check_mark:  |
| Without queue, elastic size²               | :heavy_check_mark:  |
| Bounded queue, fixed size                  | :heavy_check_mark:  |
| Bounded queue, elastic size                | :heavy_check_mark:  |
| Unbounded queue, fixed size                | :heavy_check_mark:  |
| Unbounded queue, elastic size              | :x:³                |
| Scale first, without queue, fixed size     | :x:⁴                |
| Scale first, without queue, elastic size   | :x:⁴                |
| Scale first, bounded queue, fixed size     | :x:⁵                |
| Scale first, bounded queue, elastic size   | :heavy_check_mark:⁶ |
| Scale first, unbounded queue, fixed size   | :x:⁵                |
| Scale first, unbounded queue, elastic size | :heavy_check_mark:⁶ |

¹ Core pool size = maximum pool size  
² Core pool size < maximum pool size  
³ Pool can't grow past core pool size due to unbounded queue  
⁴ Scale *first* has no meaning without a queue    
⁵ Fixed size pools are already scaled up  
⁶ Elastic, but only between **0** and maximum pool size  

#### Examples

1. Without queue, elastic size

    ```java
    ThreadPoolExecutors.builder()
        .withoutQueue()
        .elasticSize(5, 20)
        .keepAlive(1, MINUTES)
        .build()
    ```

2. Bounded queue, fixed size

    ```java
    ThreadPoolExecutors.builder()
        .boundedQueue(20)
        .fixedSize(20)
        .keepAlive(1, MINUTES)
        .build()
    ```

3. Scale-first, unbounded queue, elastic size

    ```java
    ThreadPoolExecutors.builder()
        .scaleFirst()
        .unboundedQueue()
        .elasticSize(20)   
        .keepAlive(1, MINUTES)
        .build()
    ```

You can read more about *scale-first* here:
- [Java Scale First ExecutorService — A myth or a reality](https://medium.com/@uditharosha/java-scale-first-executorservice-4245a63222df)
- [How to get the ThreadPoolExecutor to increase threads to max before queueing?](https://stackoverflow.com/questions/19528304/how-to-get-the-threadpoolexecutor-to-increase-threads-to-max-before-queueing)

In order to configure the thread pool correctly, please refer to
[How to set an ideal thread pool size](https://jobs.zalando.com/tech/blog/how-to-set-an-ideal-thread-pool-size).

### Non-blocking IO

Riptide supports two kinds of request factories:

**[`ClientHttpRequestFactory`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/client/ClientHttpRequestFactory.html)**

The following implementations offer blocking IO:

- [`ApacheClientHttpRequestFactory`](riptide-httpclient), using the [Apache HTTP Client](https://hc.apache.org/httpcomponents-client-ga/)
- ~[`HttpComponentsClientHttpRequestFactory`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/client/HttpComponentsClientHttpRequestFactory.html)~, please use the none above
- [`SimpleClientHttpRequestFactory`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/client/SimpleClientHttpRequestFactory.html), using [`HttpURLConnection`](https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html)

**[`AsyncClientHttpRequestFactory`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/client/AsyncClientHttpRequestFactory.html)**

The following implementations offer non-blocking IO:

- [`OkHttp3ClientHttpRequestFactory`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/client/OkHttp3ClientHttpRequestFactory.html), using [OkHttp](https://square.github.io/okhttp/)
- [`Netty4ClientHttpRequestFactory`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/client/Netty4ClientHttpRequestFactory.html), using [Netty](https://netty.io/)
- [`HttpComponentsAsyncClientHttpRequestFactory`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/client/HttpComponentsAsyncClientHttpRequestFactory.html), using [Apache HTTP Async Client](https://hc.apache.org/httpcomponents-asyncclient-4.1.x/index.html)

Non-blocking IO is asynchronous by nature. In order to provide asynchrony for blocking IO you need to register an executor. Not passing an executor will make all network communication synchronous, i.e. all futures returned by Riptide will already be completed.

|                 | Synchronous                | Asynchronous                            |
|-----------------|----------------------------|-----------------------------------------|
| Blocking IO     | `ClientHttpRequestFactory` | `Executor` + `ClientHttpRequestFactory` |
| Non-blocking IO | n/a                        | `AsyncClientHttpRequestFactory`         |

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
callbacks. Traditionally, you would attach different callbacks for different response status codes. Alternatively, there
are built-in routing capabilities on status code families (called series in Spring) as well as on content types. 

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

Riptide will return a `CompletableFuture<ClientHttpResponse>`. That means you can choose to chain transformations/callbacks or block
on it.

If you need proper return values take a look at [Riptide: Capture](riptide-capture).

### Exceptions

The only special custom exception you may receive is `UnexpectedResponseException`, if and only if there was no matching condition and
no wildcard condition.

### Plugins

Riptide comes with a way to register extensions in the form of plugins.

- `OriginalStackTracePlugin`, preserves stack traces when executing requests asynchronously
- [`AuthorizationPlugin`](#riptide-auth), adds `Authorization` support
- [`FailsafePlugin`](riptide-failsafe), adds retries, circuit breaker, backup requests and timeout support
- [`MicrometerPlugin`](riptide-micrometer), adds metrics for request duration
- [`TransientFaults`](riptide-faults), detects transient faults, e.g. network issues

Whenever you encounter the need to perform some repetitive task on the futures returned by a remote call,
you may consider implementing a custom Plugin for it.

Plugins are executed in phases:

[![Plugin phases](https://docs.google.com/drawings/d/e/2PACX-1vQr2WAQyNILt-UdaCL-2KbBk1QgR2MrpagpnEdI8OjD9l5aopRw2AeM7bg32feN4tutll4DAbYidjn2/pub?w=367&h=659)](https://docs.google.com/drawings/d/1zJC6533at3XzHvxsoUUqyyd4G8cZxmlJ9HFxfhBEcbg/edit?usp=sharing)

Please consult the [Plugin documentation](riptide-core/src/main/java/org/zalando/riptide/Plugin.java) for details.

### Testing

Riptide is built on the same foundation as Spring's `RestTemplate` and `AsyncRestTemplate`. That allows us, with a small
trick, to use the same testing facilities, the [`MockRestServiceServer`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/client/MockRestServiceServer.html):

```java
RestTemplate template = new RestTemplate();
MockRestServiceServer server = MockRestServiceServer.createServer(template);
ClientHttpRequestFactory requestFactory = template.getRequestFactory();

Http.builder()
    .requestFactory(requestFactory)
    // continue configuration
```

We basically use an intermediate `RestTemplate` as a holder of the special `ClientHttpRequestFactory` that the
`MockRestServiceServer` manages.

If you are using [Spring Boot Starter](riptide-spring-boot-starter), the test setup is provided by a convenient annotation `@RiptideClientTest`. 
See [here](riptide-spring-boot-starter#testing).

## Getting help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../issues).

## Getting involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change.
For more details check the [contribution guidelines](.github/CONTRIBUTING.md).

## Credits and references

- [URL routing](http://littledev.nl/?p=99)
