# Riptide: Compression

[![Zipper](../docs/zipper.jpg)](https://pixabay.com/photos/zipper-metal-gold-color-brass-201684/)

[![Build Status](https://img.shields.io/travis/zalando/riptide.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide.svg)](https://coveralls.io/r/zalando/riptide)
[![Code Quality](https://img.shields.io/codacy/grade/1fbe3d16ca544c0c8589692632d114de/master.svg)](https://www.codacy.com/app/whiskeysierra/riptide)
[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-compression.svg)](http://www.javadoc.io/doc/org.zalando/riptide-compression)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-compression.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-compression)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

*Riptide: Compression* adds support to compress request bodies.

## Features

- pluggable compression mechanism
- out of the box GZIP support

## Dependencies

- Java 8
- Riptide: Core

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-compression</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

```java
Http.builder()
    .plugin(new RequestCompressionPlugin())
    .build();
```

By default request bodies are compressed using [GZIP](https://docs.oracle.com/javase/8/docs/api/java/util/zip/GZIPOutputStream.html).

In order to specify the compression algorithm you can pass in a custom `Compression`:

```java
new RequestCompressionPlugin(Compression.of("br", BrotliOutputStream::new));
```

## Usage

```java
http.post("/events")
        .contentType(MediaType.APPLICATION_JSON)
        .body(asList(events))
        .call(pass())
        .join();
```

All request bodies will be compressed using the configured compression method using `chunked` transfer-encoding.

If there is already a `Content-Encoding` specified on the request, the plugin does nothing.

### Limitations

* You must only configure a single `RequestCompressionPlugin` as only a single encoding is applied currently.
* Starting with Spring 4.3 the `Netty4ClientHttpRequestFactory` unconditionally adds a `Content-Length` header,
which breaks if used together with  `RequestCompressionPlugin`. Use `riptide-httpclient` instead.


## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
