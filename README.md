# Riptide: Client-side response routing

[![Tidal wave](docs/wave.jpg)](http://pixabay.com/en/wave-water-sea-tsunami-giant-wave-11061/)

[![Build Status](https://img.shields.io/travis/zalando/riptide/master.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide/master.svg)](https://coveralls.io/r/zalando/riptide)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/org.zalando/riptide-core/badge.svg)](http://www.javadoc.io/doc/org.zalando/riptide-core)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-core)

> **Riptide** (ˈrɪpˌtaɪd), noun: a strong usually narrow current of water that flows away from a shore

*Riptide* is a library that implements ***client-side response routing***.  It tries to fill the gap between the HTTP
protocol and Java as a [single-dispatch](https://en.wikipedia.org/wiki/Dynamic_dispatch#Single_and_multiple_dispatch)
language. Riptide allows users to leverage the power of HTTP with its unique API.

- **Technology stack**: Based on `spring-web` and uses the same foundation as Spring's RestTemplate.
- **Status**:  Version 1.x is used in production and 2.x is currently available as a release candidate.
- Riptide is unique in the way that it doesn't abstract HTTP away, but rather embrace it!

## Example

Usage typically looks like this:

```java
http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
    .dispatch(series(),
        on(SUCCESSFUL).call(listOf(User.class), users -> 
            users.forEach(System.out::println)));
```

We have an adaptation of the canonical Github sample, see [`SampleService`](riptide-core/src/test/java/org/zalando/riptide/SampleService.java).
Feel free to compare this e.g. to [Feign](https://github.com/Netflix/feign#basics) or
[Retrofit](https://github.com/square/retrofit/blob/master/samples/src/main/java/com/example/retrofit/SimpleService.java).

## Features

- full access to the underlying HTTP client
- encourages to write more resilient clients, by forcing you to consider
  - fallbacks
  - content negotiation and versioning
  - robust error handling
- elegant syntax
- type-safe

## Origin

Most modern clients try to adapt HTTP to single-dispatch paradigm like shown in the following example. Even though this
may be perfectly suitable for most applications it takes away a lot of the power that comes with HTTP. It's not easy to
support multiple different return values, i.e. distinct happy cases. Access to response headers or manual content
negotiation are also harder to do.
 
```java
@GET
@Path("/repos/{org}/{repo}/contributors")
List<User> getContributors(@PathParam String org, @PathParam String repo);
```
Riptide tries to counter this by providing a different approach to leverage the power of HTTP.

## Concepts

It allows to dispatch HTTP responses very easily to different handler methods based on any characteristic of the
response, including but not limited to status code, status family and content type. The way this works is intentionally
very similar to server-side request routing where any request that reaches a web application is usually routed to the
correct handler based on any combination of the following criteria: URI including query and path parameters, method,
`Accept` and `Content-Type` header. Instead of routing requests to handler methods on the server what *Riptide* does
is the exact opposite: routing responses to handler methods on the client side.

![Routing Tree](https://docs.google.com/drawings/d/1BRTXVtmwIMJti1l5cQMrZsfKnTfBElTB8pDSxVBQbIQ/pub?w=888&h=691)

### Route

> A Route is either a user-supplied **callback or** a nested **[routing tree](#routing-tree)**. Following a route will
  execute the callback or traverse the routing tree respectively.

```java
on(SUCCESSFUL).call(response -> {
    System.out.println(response.getHeaders().getLocation());
}),
```

### Routing Tree

> A Routing Tree is the combination of a **[navigator](#navigator) and** a set of **[bindings](#binding)**.

```java
on(SUCCESSFUL).dispatch(contentType(),
    on(APPLICATION_JSON).call(..),
    on(APPLICATION_XML).call(..))
```

### Navigator

> A Navigator **chooses among** the **[bindings](#binding)** of a [routing tree](#routing-tree).

| Navigator                                                                                              | Aspect               |
|--------------------------------------------------------------------------------------------------------|----------------------|
| [Navigators.series()](riptide-core/src/main/java/org/zalando/riptide/SeriesNavigator.java)             | Class of status code |
| [Navigators.status()](riptide-core/src/main/java/org/zalando/riptide/StatusNavigator.java)             | Status               |
| [Navigators.statusCode()](riptide-core/src/main/java/org/zalando/riptide/StatusCodeNavigator.java)     | Status code          |
| [Navigators.reasonPhrase()](riptide-core/src/main/java/org/zalando/riptide/ReasonPhraseNavigator.java) | Reason Phrase        |
| [Navigators.contentType()](riptide-core/src/main/java/org/zalando/riptide/ContentTypeNavigator.java)   | Content-Type header  |

### Binding

> A Binding **binds an attribute to a [route](#route)**.

| Route                                  | Syntax                                              |
|----------------------------------------|-----------------------------------------------------|
| `ThrowingRunnable`                     | `on(..).call(ThrowingRunnable)`                     |
| `ThrowingConsumer<ClientHttpResponse>` | `on(..).call(ThrowingConsumer<ClientHttpResponse>)` |
| `ThrowingConsumer<T>`                  | `on(..).call(Class<T>, ThrowingConsumer<T>)`        |
| `ThrowingConsumer<T>`                  | `on(..).call(TypeToken<T>, ThrowingConsumer<T>)`    |
| `RoutingTree`                          | `on(..).dispatch(..)`                               |

### Nested Dispatch

> A nested dispatch is the act of **traversing a [routing tree](#routing-tree)** by letting the [navigator](#navigator)
  choose a binding and follow its associated route.

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

## Configuration

```java
Rest.builder()
    .baseUrl("https://api.github.com")
    .requestFactory(new HttpComponentsAsyncClientHttpRequestFactory())
    .converter(new MappingJackson2HttpMessageConverter())
    .converter(new Jaxb2RootElementHttpMessageConverter())
    .build();
```

Since all properties are optional the following code is the bare minimum:

```java
Rest.builder().build();
```

This defaults to:
- no base URL
- `SimpleClientHttpRequestFactory` (based on `java.net.HttpURLConnection`)
- same list of converters as `new RestTemplate()`

## Usage

A full-blown request may contain any of the following aspects: HTTP method, request URI, query parameters,
headers and a body:

```java
http.post("/sales-order")
    .queryParam("async", "false")
    .contentType(CART)
    .accept(SALES_ORDER)
    .header("Client-IP", "127.0.0.1")
    .body(cart)
    .dispatch(series(),
        on(SUCCESSFUL).dispatch(contentType(),
            on(SALES_ORDER).call(this::persistLocationHeader),
        on(CLIENT_ERROR).dispatch(status(),
            on(CONFLICT).call(this::retry),
            on(PRECONDITION_FAILED).call(this::readAgainAndRetry),
            anyStatus().dispatch(contentType(),
                on(MediaTypes.PROBLEM_JSON).call(ThrowableProblem.class, propagate()))),
        on(SERVER_ERROR).dispatch(status(),
            on(SERVICE_UNAVAILABLE).call(this::scheduleRetryLater))))
    .join();
```

Riptide the the following HTTP methods: `get`, `head`, `post`, `put`, `patch`, `delete`, `options` and `trace`
respectively. Query parameters can either be provided individually using `queryParam(String, String)` or multiple at 
once with `queryParams(Multimap<String, String>)`. The `Content-Type`- and `Accept`-header have type-safe methods in
addition to the generic support that is `header(String, String)` and `headers(HttpHeaders)`.

The callbacks used can have the following signatures:

```java
private void persistLocationHeader(ClientHttpResponse response)
private void retry();
private void propagate(ThrowableProblem problem);
```

### Futures and Completion

- TODO future/blocking/chaining

### Exceptions

*Riptide* propagates any exception as an `ExecutionException` upon calling `Future.get(..)`:

```java
try {
    rest.execute(GET, url).dispatch(..).get(10, SECONDS);
} catch (final ExecutionException e) {
    // TODO implement
}
```

TODO Alternatively

```java
try {
    rest.execute(GET, url).dispatch(..).join;
} catch (final CompletionException e) {
    // TODO implement
}
```

The only special custom exception you may get is `NoRouteException`, if and only if there was no matching condition and
no wildcard condition either.

## Getting help

If you have questions, concerns, bug reports, etc, please file an issue in this repository's Issue Tracker.

## Getting involved

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change.
For more details check the [contribution guidelines](CONTRIBUTING.md).

## Credits and references

- [URL routing](http://littledev.nl/?p=99)
