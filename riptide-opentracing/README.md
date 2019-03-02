# Riptide: OpenTracing

[![Spider web](../docs/spider-web.jpg)](https://pixabay.com/photos/cobweb-drip-water-mirroring-blue-3725540/)

[![Build Status](https://img.shields.io/travis/zalando/riptide/master.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide/master.svg)](https://coveralls.io/r/zalando/riptide)
[![Code Quality](https://img.shields.io/codacy/grade/1fbe3d16ca544c0c8589692632d114de/master.svg)](https://www.codacy.com/app/whiskeysierra/riptide)
[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-metrics.svg)](http://www.javadoc.io/doc/org.zalando/riptide-metrics)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-metrics.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-metrics)
[![OpenTracing](https://img.shields.io/badge/OpenTracing-enabled-blue.svg)](http://opentracing.io)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

*Riptide: OpenTracing* adds sophisticated [OpenTracing](https://opentracing.io/) support to *Riptide*.

## Example

```java
Http.builder()
    .plugin(new OpenTracingPlugin(tracer))
    .build();
```

## Features

- Client span lifecycle management
- Span context injection into HTTP headers of requests
- Extensible span decorators for tags and logs
- Seamless integration with [Riptide: Failsafe](../riptide-failsafe)

## Dependencies

- Java 8
- Riptide Core
- [OpenTracing Java API](https://opentracing.io/guides/java/)
- [Riptide: Failsafe](../riptide-failsafe) (optional)

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-opentracing</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

```java
Http.builder()
    .baseUrl("https://www.example.com")
    .plugin(new OpenTracingPlugin(tracer))
    .build();
```

The following tags/logs are supported out of the box:

| Tag/Log Field        | Decorator                      | Example                           |
|----------------------|--------------------------------|-----------------------------------|
| `component`          | `ComponentSpanDecorator`       | `Riptide`                         |
| `span.kind`          | `SpanKindSpanDecorator`        | `client`                          |
| `peer.hostname`      | `PeerSpanDecorator`            | `www.github.com`                  |
| `peer.port`          | `PeerSpanDecorator`            | `80`                              |
| `http.method`        | `HttpMethodSpanDecorator`      | `GET`                             |
| `http.url`           | `HttpUrlSpanDecorator`         | `https://www.github.com/users/me` |
| `http.path`          | `HttpPathSpanDecorator`        | `/users/{user_id}`                |
| `http.status_code`   | `HttpStatusCodeSpanDecorator`  | `200`                             |
| `error`              | `ErrorSpanDecorator`           | `false`                           |
| `error.kind` (log)   | `ErrorSpanDecorator`           | `SocketTimeoutException`          |
| `error.object` (log) | `ErrorSpanDecorator`           | (exception instance)              |
| `retry`              | `RetrySpanDecorator`           | `true`                            |
| `retry_number` (log) | `RetrySpanDecorator`           | `3`                               |
| `*`                  | `CallSiteSpanDecorator`        | `admin=true`                      |
| `*`                  | `StaticTagSpanDecorator`       | `aws.region=eu-central-1`         |
| `*`                  | `UriVariablesTagSpanDecorator` | user_id=me                        |

### Notice

**Be aware**: The `http.url` tag is disabled by default because the full request URI may contain
sensitive, [*personal data*](https://en.wikipedia.org/wiki/General_Data_Protection_Regulation).
As an alternative we introduced the `http.path` tag which favors the URI template over the
already expanded version. That has the additional benefit of a significant lower cardinality
compared to what `http.url` would provide. 

If you still want to enable it, you can do so by just registering the missing span decorator:

```java
new OpenTracingPlugin(tracer)
    .withAdditionalSpanDecorators(new HttpUrlSpanDecorator())
```

### Span Decorators

Span decorators are a simple, yet powerful tool to manipulate the span, i.e. they allow you to
add tags, logs and baggage to spans. The default set of decorators can be extended by using 
`OpenTracingPlugin#withAdditionalSpanDecorators(..)`:

```java
new OpenTracingPlugin(tracer)
    .withAdditionalSpanDecorators(new StaticSpanDecorator(singletonMap(
            "environment", "local"
    )))
```

If the default span decorators are not desired you can replace them completely using
`OpenTracingPlugin#withSpanDecorators(..)`:

```java
new OpenTracingPlugin(tracer)
        .withSpanDecorators(
            new ComponentSpanDecorator("MSIE"),
            new SpanKindSpanDecorator(Tags.SPAN_KIND_CONSUMER),
            new PeerSpanDecorator(),
            new HttpMethodSpanDecorator(),
            new HttpPathSpanDecorator(),
            new HttpUrlSpanDecorator(),
            new HttpStatusCodeSpanDecorator(),
            new ErrorSpanDecorator(),
            new CallSiteSpanDecorator())
```

## Usage

Typically you won't need to do anything at the call-site regarding OpenTracing, i.e.
your usages of Riptide should work exactly as before:

```java
http.get("/users/{id}", userId)
    .dispatch(series(),
        on(SUCCESSFUL).call(User.class, this::greet),
        anySeries().call(problemHandling()))
```

### Operation Name

By default the HTTP method will be used as the operation name, which might not fit your needs.
Since deriving a meaningful operation name from request arguments alone is unreliable, you can
specify the `OpenTracingPlugin.OPERATION_NAME` request attribute to override the default:

```java
http.get("/users/{id}", userId)
    .attribute(OpenTracingPlugin.OPERATION_NAME, "get_user")
    .dispatch(series(),
        on(SUCCESSFUL).call(User.class, this::greet),
        anySeries().call(problemHandling()))
```

### Call-Site Tags

Assuming you have the [`CallSiteSpanDecorator`](#span-decorators) registered (it is by default), you can also
specify custom tags based on context information which wouldn't be available within the plugin
anymore:

```java
http.get("/users/{id}", userId)
    .attribute(OpenTracingPlugin.TAGS, singletonMap("retry", "true"))
    .dispatch(series(),
        on(SUCCESSFUL).call(User.class, this::greet),
        anySeries().call(problemHandling()))
```

### URI Variables as Tags

URI templates are not just safer to use (see [Configuration](#notice)), they can also be used to
generate tags from URI variables. Given you have the `UriVariablesTagSpanDecorator` registered
then the following will produce a `user_id=123` tag:

```java
http.get("/users/{user_id}", 123)
```

The same warning applies as mentioned before regarding [`http.url`](#notice). This feature may
expose *personal data* and should be used with care.

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply open a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
