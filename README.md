# Riptide

[![Tidal wave](docs/wave.jpg)](http://pixabay.com/en/wave-water-sea-tsunami-giant-wave-11061/)

[![Build Status](https://img.shields.io/travis/zalando/riptide.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide.svg)](https://coveralls.io/r/zalando/riptide)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide)

A response router for Spring's RestTemplate. Riptide adds a customizable dispatcher on top of
Spring's RestTemplate that allows you to handle different status codes, content types, etc.
differently with an easy to use yet very powerful syntax.

## Dependency

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Setup

Create an instance based on an existing `RestTemplate`:

```java
final RestTemplate template = new RestTemplate();
template.setErrorHandler(new PassThroughResponseErrorHandler());
final Rest rest = Rest.create(template);
```

If you use Riptide to its full extent you probably don't want to have any [`ResponseErrorHandler`]
(http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/ResponseErrorHandler.html)
interfere with your dispatching. Therefore Riptide provides you with a *no-op* `ResponseErrorHandler`, which ensures
that Riptide handles all success and error cases.

In case you're using the **OAuth2RestTemplate**: It uses the given `ResponseErrorHandler` in the wrong way, which
may result in the response body being already consumed and/or closed. To workaround this issue use this special
`OAuth2CompatibilityResponseErrorHandler`:

```java
template.setErrorHandler(new OAuth2CompatibilityResponseErrorHandler());
```

## Usage

Make a request and route the response to your specific handler methods/callbacks:

```java
rest.execute(GET, url).dispatch(status(),
        on(CREATED, Success.class).call(this::onSuccess),
        on(ACCEPTED, Success.class).call(this::onSuccess),
        on(BAD_REQUEST).call(this::onError),
        anyStatus().call(this::fail));
```

You `onSuccess` method is allowed to have one of the following signatures:

```java
void onSuccess(Success success) throws Exception;
void onSuccess(ResponseEntity<Success> success) throws Exception;
```

The later one is useful if you e.g. need access to one or more header values.

## Selectors

Routing of responses is controlled by a `Selector`, e.g. `status()` in the former example.
A selector selects the attribute of a response you want to use to route it.

Riptide comes with the following selectors:

| Selector                                                                                                                                   | Attribute                                                                                                                  |
|--------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| [Selectors.series()](https://github.com/whiskeysierra/riptide/blob/master/src/main/java/org/zalando/riptide/SeriesSelector.java)           | [HttpStatus.Series](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/HttpStatus.Series.html) |
| [Selectors.status()](https://github.com/whiskeysierra/riptide/blob/master/src/main/java/org/zalando/riptide/StatusSelector.java)           | [HttpStatus](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/HttpStatus.html)               |
| [Selectors.statusCode()](https://github.com/whiskeysierra/riptide/blob/master/src/main/java/org/zalando/riptide/StatusCodeSelector.java)   | [Integer](http://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html)                                                 |
| [Selectors.contentType()](https://github.com/whiskeysierra/riptide/blob/master/src/main/java/org/zalando/riptide/ContentTypeSelector.java) | [MediaType](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/MediaType.html)                 |

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

## Conditions

[Conditions](https://github.com/whiskeysierra/riptide/blob/master/src/main/java/org/zalando/riptide/Conditions.java)
describe which concrete attribute values you want to bind to which actions.

```java
on(SUCCESS).call(..)
on(CLIENT_ERROR, Error.class).call(..)
anySeries().call(..)
```

Conditions can either be:

1. *untyped*, e.g. `on(SUCCESS)`, 
2. *typed*, e.g. `on(CLIENT_ERROR, Error.class)` or 
3. *wildcards*, e.g. `anySeries()`. 

Untyped conditions only support untyped actions, i.e. actions that operate on a low-level
[`ClientHttpResponse`](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/client/ClientHttpResponse.html)
while typed conditions support typed actions, i.e. actions that operate on custom types or typed
[`ResponseEntity`](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/ResponseEntity.html)
directly.

Wildcard conditions are comparable to a `default` case in a switch. They take effect if no match was found.

## Actions

After the selector determined the attribute, the condition matched on a concrete attribute value the
response will be routed to an action. An action can be one of the following:

| Action                                      | Syntax                         |
|---------------------------------------------|--------------------------------|
| `Consumer<ClientHttpResponse>`              | `on(..).call(..)`              |
| `Consumer<ResponseEntity<T>>`               | `on(.., ..).call(..)`          |
| `Consumer<T>`                               | `on(.., ..).call(..)`          |
| `Function<ClientHttpResponse, ?>` + capture | `on(..).map(..).capture()`     |
| `Function<ResponseEntity<T>, ?>` + capture  | `on(.., ..).map(..).capture()` |
| `Function<T, ?>` + capture                  | `on(.., ..).map(..).capture()` |
| Nested Routing                              | see next section               |

Consumers can be used to trigger some dedicated function and they work well if no return value is required.
Functions on the other hand are used to apply a transformation and their result must be captured. Captured values can 
later be retrieved, e.g. to produce a return value:

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

Please note: All consumer/function based actions are **not** `java.util.function.Consumer` and 
`java.util.function.Function` respectively, but custom version that support throwing checked exceptions. This should
not have any negative impact since most of the time you won't pass in a custom implementation, but rather a lambda or
a method reference.

### Nested Routing

A special action is the *nested routing* which allows to have a very fine-grained control over how to route your
responses:

```java
final Success success = rest.execute(GET, url)
        .dispatch(series(),
                on(SUCCESSFUL)
                        .dispatch(status(),
                                on(CREATED, Success.class).capture(),
                                on(ACCEPTED, Success.class).capture(),
                                anyStatus().call(this::fail)),
                on(CLIENT_ERROR)
                    .dispatch(status(),
                            on(UNAUTHORIZED).call(this::login),
                            on(UNPROCESSABLE_ENTITY)
                                    .dispatch(contentType(),
                                            on(PROBLEM, Problem.class).capture(),
                                            on(ERROR, Problem.class).capture(),
                                            anyContentType().call(this::fail)),
                            anyStatus().call(this::fail)),
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

## Exceptions

*Riptide* propagates any exception thrown by the underlying `RestTemplate` or any of the custom callbacks passed to `call` or `map` *as-is*, which means if you're interested in any of those, you can put the call to `Rest.execute(..)` in a `try-catch` and directly catch it.

The only special custom exception you may get is `NoRouteException`, if and only if there was no matching condition and no wildcard condition either.

## License

Copyright [2015] Zalando SE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
