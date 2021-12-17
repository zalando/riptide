# Riptide: OpenTelemetry

[![Spider web](../docs/spider-web.jpg)](https://pixabay.com/photos/cobweb-drip-water-mirroring-blue-3725540/)

[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-micrometer.svg)](http://www.javadoc.io/doc/org.zalando/riptide-micrometer)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-opentelemetry.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-opentelemetry)

*Riptide: OpenTelemetry* adds sophisticated [OpenTelemetry](https://opentelemetry.io/) support to *Riptide*.

## Example

```java
Http.builder()
    .plugin(new OpenTelemetryPlugin(tracer))
    .build();
```

## Features

- Span context injection into HTTP headers of requests
- Extensible span decorators for attributes
- Seamless integration with [Riptide: Failsafe](../riptide-failsafe)

## Dependencies

- Java 8
- Riptide Core
- [OpenTelemetry Java API](https://opentelemetry.io/docs/instrumentation/java/)
- [Riptide: Failsafe](../riptide-failsafe) (optional)

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-opentelemetry</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

```java
Http.builder()
    .baseUrl("https://www.example.com")
    .plugin(new OpenTelemetryPlugin(tracer)
        .withAdditionalSpanDecorators(new HttpUrlSpanDecorator))
    .build();
```

A new span will be started for each request and it will be activated.

The following tags are supported out of the box:

| Attribute Field              | Decorator                          | Example                           |
|------------------------------|------------------------------------|-----------------------------------|
|                              | `CompositeSpanDecorator`¹          |                                   |
| `exception.type`             | `ErrorSpanDecorator`               | `SocketTimeoutException`          |
| `exception.message`          | `ErrorSpanDecorator`               | `Socket timed out`                |
| `exception.stacktrace`       | `ErrorSpanDecorator`               | `SocketTimeoutException at [...]` |
| `http.host`                  | `HttpHostSpanDecorator`            | `www.github.com`                  |
| `http.method`                | `HttpMethodSpanDecorator`          | `POST`                            |
| `http.path`                  | `HttpPathSpanDecorator`            | `/users/{user_id}`                |
| `http.status`                | `HttpStatusCodeSpanDecorator`      | `200`                             |
| `peer.hostname`              | `PeerHostSpanDecorator`            | `www.github.com`                  |
| `retry`                      | `RetrySpanDecorator`²              | `true`                            |
| `retry_number`               | `RetrySpanDecorator`²              | `3`                               |
|                              | `StaticTagSpanDecorator`²          | `zone=aws:eu-central-1a`          |

¹ The `CompositeSpanDecorator` allows to treat multiple decorators as one. 
² **Not** registered by default.

### Span Decorators

Span decorators are a simple, yet powerful tool to manipulate the span, i.e. they allow you to add attributes to the spans. 
The default set of decorators can be extended by using `OpenTracingPlugin#withAdditionalSpanDecorators(..)`:

```java
new OpenTelemetryPlugin(tracer, new StaticSpanDecorator(singletonMap(
            "environment", "local"
    )))
```

## Usage

Typically, you won't need to do anything at the call-site regarding OpenTelemetry, i.e. your usages of Riptide should work exactly as before:

```java
http.get("/users/{id}", userId)
    .dispatch(series(),
        on(SUCCESSFUL).call(User.class, this::greet),
        anySeries().call(problemHandling()))
```

### Operation Name

By default, the HTTP method will be used as the operation name, which might not fit your needs.
Since deriving a meaningful operation name from request arguments alone is unreliable, you can specify the `OpenTelemetryPlugin.OPERATION_NAME` request attribute to override the default:

```java
http.get("/users/{id}", userId)
    .attribute(OpenTelemetryPlugin.OPERATION_NAME, "get_user")
    .dispatch(series(),
        on(SUCCESSFUL).call(User.class, this::greet),
        anySeries().call(problemHandling()))
```

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply open a pull request and add a brief description (1-2 sentences) of your addition or change.
For more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
