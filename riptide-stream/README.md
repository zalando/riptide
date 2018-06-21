# Riptide: Stream

[![Waterfall](../docs/waterfall.jpg)](https://pixabay.com/en/waterfalls-river-stream-water-691917/)

[![Build Status](https://img.shields.io/travis/zalando/riptide.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide.svg)](https://coveralls.io/r/zalando/riptide)
[![Code Quality](https://img.shields.io/codacy/grade/1fbe3d16ca544c0c8589692632d114de/master.svg)](https://www.codacy.com/app/whiskeysierra/riptide)
[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-stream.svg)](http://www.javadoc.io/doc/org.zalando/riptide-stream)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-capture.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-stream)

*Riptide: Stream* allows to read arbitrary infinite JSON streams.

## Example

```java
http.get("/sales-orders")
    .dispatch(series(),
        on(SUCCESSFUL).call(streamOf(Order.class), forEach(this::process)));
```

## Features

- HTTP streaming
- no direct Riptide dependency
  - can be used with a plain `RestTemplate`
- supports
  - [`application/json-seq`](https://tools.ietf.org/html/rfc7464)
  - [`application/stream+json`](https://tools.ietf.org/id/draft-snell-activity-streams-type-01.html)
  - `application/x-json-stream`
- type-safe

## Dependencies

- Java 8

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-stream</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

In order to enable streaming you only need to register the
[`StreamConverter`](src/main/java/org/zalando/riptide/stream/StreamConverter.java). You're also strongly encouraged to
use `RestAsyncClientHttpRequestFactory` provided by [*Riptide: HTTP Client*](../riptide-httpclient) as it fixes an
[issue with Spring's implementation](https://jira.spring.io/browse/SPR-14882) which tries to consume infinite streams when trying to close a connection.

```java
Http.builder()
    .requestFactory(new RestAsyncClientHttpRequestFactory(client, executor))
    .converter(Streams.streamConverter(mapper))
    .build();
```

## Usage

You can either consume a stream using `forEach(Consumer)`, as shown in the first example.
Alternatively you can capture and return a stream:

```java
public Stream<Order> streamOrders() {
    Capture<Stream<Order>> capture = Capture.empty();
    
    return http.get("/sales-orders")
        .dispatch(series(),
            on(SUCCESSFUL).call(streamOf(Order.class), capture))
        .thenApply(capture)
        .join();
}
```

**Beware**, the returned stream has to be closed properly otherwise it may occupy a connection/socket forever. This 
might be easy to miss since most streams are backed by collections and don't need to be closed explicitly:

> Streams have a BaseStream.close() method and implement AutoCloseable, but nearly all stream instances do not actually
need to be closed after use. Generally, only streams whose source is an IO channel (such as those returned by
Files.lines(Path, Charset)) will require closing. Most streams are backed by collections, arrays, or generating
functions, which require no special resource management. (If a stream does require closing, it can be declared as a
resource in a try-with-resources statement.)
>
> https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
