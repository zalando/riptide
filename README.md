# Riptide

[![Tidal wave](docs/wave.jpg)](http://pixabay.com/en/wave-water-sea-tsunami-giant-wave-11061/)

[![Build Status](https://img.shields.io/travis/zalando/riptide.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide.svg)](https://coveralls.io/r/zalando/riptide)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide)

*Riptide* is an extension to Spring's [RestTemplate](https://spring.io/guides/gs/consuming-rest/) that offers 
what we call ***client-side response routing***.

It allows to dispatch HTTP responses very easily to different handler methods based on any characteristic of the
response, including but not limited to status code, status family and content type. The way this works is intentionally
very similar to server-side request routing where any request that reaches a web application is usually routed to the
correct handler based on any combination of the following criteria: URI including query and path parameters, method, 
`Accept` and `Content-Type` header. Instead of routing requests to handler methods on the server what *Riptide* does
is the exact opposite: routing responses to handler methods on the client side.

## Features

- thin wrapper around RestTemplate
- full access to the underlying HTTP client
- encourages to write more resilient clients, by forcing you to consider
  - fallbacks
  - content negotiation
  - robust error handling
- elegant syntax
- type-safety
- easy to implement repeating patterns, e.g.
  - follow redirects
  - create resource and retrieve location

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

Create an instance based on an existing `RestTemplate`:

```java
final RestTemplate template = new RestTemplate();
template.setErrorHandler(new PassThroughResponseErrorHandler());
final Rest rest = Rest.create(template);
```

Or alternatively an `AsyncRestTemplate`:

```java
final AsyncRestTemplate template = new AsyncRestTemplate();
template.setErrorHandler(new PassThroughResponseErrorHandler());
final AsyncRest rest = AsyncRest.create(template);
```

If you use Riptide to its full extent you probably don't want to have any [`ResponseErrorHandler`]
(http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/ResponseErrorHandler.html)
interfere with your dispatching. Therefore Riptide provides you with a *no-op* `ResponseErrorHandler`, which ensures
that Riptide handles all success and error cases.

**BEWARE** In case you're using the `OAuth2RestTemplate`: It uses the given `ResponseErrorHandler` in the wrong way,
which may result in the response body being already consumed and/or closed. To workaround this issue use our special
`OAuth2CompatibilityResponseErrorHandler` instead.

## Usage

Make a request and route the response to your specific handler methods/callbacks:

```java
rest.execute(GET, url).dispatch(status(),
        on(CREATED, Success.class).call(this::onSuccess),
        on(ACCEPTED, Success.class).call(this::onSuccess),
        on(BAD_REQUEST).call(this::onError),
        anyStatus().call(this::fail));
```

Your `onSuccess` method is allowed to have one of the following signatures:

```java
void onSuccess(Success success) throws Exception;
void onSuccess(ResponseEntity<Success> success) throws Exception;
```

The later one is useful if you e.g. need access to one or more header values.

### Selectors

Routing of responses is controlled by a `Selector`, e.g. `status()` in the former example.
A selector selects the attribute(s) of a response you want to use to route it.

Riptide comes with the following selectors:

| Selector                                                                              | Attribute                                                                                                                  |
|---------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| [Selectors.series()](src/main/java/org/zalando/riptide/SeriesSelector.java)           | [HttpStatus.Series](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/HttpStatus.Series.html) |
| [Selectors.status()](src/main/java/org/zalando/riptide/StatusSelector.java)           | [HttpStatus](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/HttpStatus.html)               |
| [Selectors.statusCode()](src/main/java/org/zalando/riptide/StatusCodeSelector.java)   | [Integer](http://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html)                                                 |
| [Selectors.contentType()](src/main/java/org/zalando/riptide/ContentTypeSelector.java) | [MediaType](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/MediaType.html)                 |

```java
rest.execute(..).dispatch(series(), ..);
rest.execute(..).dispatch(status(), ..);
rest.execute(..).dispatch(statusCode(), ..);
rest.execute(..).dispatch(contentType(), ..);
```

You are free to write your own, which requires you to just implement this single method:

```java
Optional<A> attributeOf(ClientHttpResponse response)
```

An attribute can be a single scalar value but could be a complex type, based on your needs.

### Conditions

[Conditions](src/main/java/org/zalando/riptide/Conditions.java)
describe which concrete attribute values you want to bind to which actions.

```java
on(SUCCESS).call(..)
on(CLIENT_ERROR, Problem.class).call(..)
anySeries().call(..)
```

Conditions can either be:

1. *untyped*, e.g. `on(SUCCESS)`, 
2. *typed*, e.g. `on(CLIENT_ERROR, Problem.class)` or 
3. *wildcards*, e.g. `anySeries()`. 

Untyped conditions only support untyped actions, i.e. actions that operate on a low-level
[`ClientHttpResponse`](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/client/ClientHttpResponse.html)
while typed conditions support typed actions, i.e. actions that operate on custom types or typed
[`ResponseEntity`](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/ResponseEntity.html)
directly.

Wildcard conditions are comparable to a `default` case in a switch. They take effect if no match was found. They are a
very powerful tool in being a resilient client, i.e. you should consider to always have one wildcard condition to
catch cases where the server introduced a new status code or content type. That way you can declare a meaningful
handling of those cases upfront already.

### Actions

After the selector determined the attribute, the condition matched on a concrete attribute value the
response will be routed to an action. An action can be one of the following types:

| Action                                      | Syntax                         |
|---------------------------------------------|--------------------------------|
| `Consumer<ClientHttpResponse>`              | `on(..).call(..)`              |
| `Runnable`                                  | `on(..).call(..)`              |
| `Consumer<ResponseEntity<T>>`               | `on(.., ..).call(..)`          |
| `Consumer<T>`                               | `on(.., ..).call(..)`          |
| `Function<ClientHttpResponse, ?>` + capture | `on(..).map(..).capture()`     |
| `Function<ResponseEntity<T>, ?>` + capture  | `on(.., ..).map(..).capture()` |
| `Function<T, ?>` + capture                  | `on(.., ..).map(..).capture()` |
| Nested Routing                              | `on(..).dispatch(..)`          |

Consumers can be used to trigger some dedicated function and they work well if no return value is required.

Functions on the other hand are used to apply a transformation and their result must be captured. Captured values can 
later be retrieved, e.g. to produce a return value. Please be aware that captures are not available when using
`AsyncRest`.

```java
final Optional<Success> success = rest.execute(..)
        .dispatch(..)
        .as(Success.class);

return success.orElse(..);
```

Alternatively, if your dispatching doesn't allow multiple happy cases, you can retrieve a value directly, without
dealing with an `Optional`:

```java
return rest.execute(..)
        .dispatch(..)
        .to(Success.class);
```

Please note: All consumer/function based actions are **not** `java.util.function.Consumer`, `java.lang.Runnable` and
`java.util.function.Function` respectively, but custom version that support throwing checked exceptions. This should
not have any negative impact since most of the time you won't pass in a custom implementation, but rather a lambda or
a method reference.

#### Nested Routing

A special action is the *nested routing* which allows to have a very fine-grained control over how to route your
responses:

```java
Success success = rest.execute(GET, url)
        .dispatch(series(),
                on(SUCCESSFUL, Success.class).capture(),
                on(CLIENT_ERROR)
                    .dispatch(status(),
                            on(UNAUTHORIZED).call(this::login),
                            on(UNPROCESSABLE_ENTITY)
                                    .dispatch(contentType(),
                                            on(PROBLEM, ThrowableProblem.class).call(propagate()),
                                            on(ERROR, Exception.class).call(propagate()))),
                on(SERVER_ERROR)
                    .dispatch(statusCode(),
                            on(503).call(this::retryLater),
                anySeries().call(this::fail))
        .as(Success.class).orElse(null);
```

If a *no match* case happens in a nested routing scenario it will bubble up the levels until it finds a matching
wildcard condition. In the example above, if the server responded with a plain `500 Internal Server Error` the
router would dispatch on the series, entering `on(SERVER_ERROR)` (5xx), try to dispatch on status code, won't find a
matching condition and neither a wildcard so it would bubble up and be *caught* by the `anySeries().call(..)`
statement.

### Patterns and examples

This section contains some ready to be used patterns and examples on how to solve certain challenges using Riptide: 

#### Follow Redirects

```java
private void send(URI url, T body) {
    rest.execute(POST, url, body).dispatch(series(),
            on(SUCCESSFUL).call(pass()),
            on(REDIRECTION).call(response ->
                    send(response.getHeaders().getLocation(), body)),
            anySeries().call(this::fail));
}
```

#### Create resource and retrieve location

```java
private URI create(URI url, T body) {
    return rest.execute(POST, url, body).dispatch(series(),
            on(SUCCESSFUL).capture(location()),
            anySeries().call(this::fail))
            .to(URI.class);
}
```

### Exceptions

*Riptide* propagates any exception thrown by the underlying `RestTemplate` or any of the custom callbacks passed to 
`call` or `map` *as-is*, which means if you're interested in any of those, you can put the call to `Rest.execute(..)` 
in a `try-catch` and directly catch it. When using `AsyncRest` a traditional `try-catch` wouldn't work, there is a 
special syntax for it:

```java
rest.execute(GET, url).dispatch(status(), route(
        on(CREATED, Success.class).call(this::onSuccess),
        on(ACCEPTED, Success.class).call(this::onSuccess),
        on(BAD_REQUEST).call(this::onError),
        anyStatus().call(this::fail)),
        handle(e -> LOG.error("Failed to execute asynchronous request", e));
```

Notable differences between the signatures of `dispatch` in `Rest` and `AsyncRest`:

1. Bindings are provided as a List, instead of varargs
2. Exception handling is done inside a `FailureCallback`

```java
<A> Capture dispatch(Selector<A> selector, Binding<A>... bindings);
<A> void dispatch(Selector<A> selector, List<Binding<A>> bindings, FailureCallback callback);
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
