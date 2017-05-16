# Riptide: Streams

[![Waterfall](../docs/waterfall.jpg)](https://pixabay.com/en/waterfalls-river-stream-water-691917/)

[![Build Status](https://img.shields.io/travis/zalando/riptide.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide.svg)](https://coveralls.io/r/zalando/riptide)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/org.zalando/riptide-stream/badge.svg)](http://www.javadoc.io/doc/org.zalando/riptide-stream)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-capture.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-stream)

*Riptide Stream* extension allows to capture arbitrary infinite object streams via Spring's [RestTemplate](https://spring.io/guides/gs/consuming-rest/).
This includes infinite streaming format as application/x-json-stream and application/json-seq, but also streaming of
simple finite lists/arrays of JSON objects.

## Example

```java
http.get("/sales-orders")
    .dispatch(series(),
        on(SUCCESSFUL).call(streamOf(Order.class), forEach(this::process)));
```

## Features

- HTTP streaming
- type-safe

## Dependencies

- Java 8
- Riptide: Core

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-stream</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Usage

To enable streaming you only need to register the
[stream converter](src/main/java/org/zalando/riptide/stream/StreamConverter.java) with Riptide and declare a route for
your stream that is calling a the stream consumer.

The unique entry point for all specific methods is the [Streams](src/main/java/org/zalando/riptide/stream/Streams.java)
class.

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../CONTRIBUTING.md).
