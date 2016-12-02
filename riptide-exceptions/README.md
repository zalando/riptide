# Riptide: Exceptions

[![Shipwreck](../docs/shipwreck.jpg)](https://commons.wikimedia.org/wiki/File:2008-12-15_Lanzarote_Wreck.jpg)

[![Build Status](https://img.shields.io/travis/zalando/riptide.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide.svg)](https://coveralls.io/r/zalando/riptide)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/org.zalando/riptide-exceptions/badge.svg)](http://www.javadoc.io/doc/org.zalando/riptide-exceptions)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-exceptions.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-exceptions)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

*Riptide: Exceptions* helps to classify exceptions into *permant* and *temporary* errors.

## Features

- exception classification
- easier exception handling, e.g. retries
- reusable

## Dependencies

- Java 8
- Riptide: Core

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-exceptions</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

```java
Rest.builder()
    .plugin(new TemporaryExceptionPlugin())
    .build();
```

By default the following exception types are classified as temporary:

- [`InterruptedIOException`](https://docs.oracle.com/javase/8/docs/api/java/io/InterruptedIOException.html)
- [`SocketException`](https://docs.oracle.com/javase/8/docs/api/java/net/SocketException.html)
- [`SSLHandshakeException`](https://docs.oracle.com/javase/8/docs/api/javax/net/ssl/SSLHandshakeException.html)

In order to change this you can pass in a custom `Classifier`:

```java
Classifier classifier = Classifier.create(IOException.class::isInstance);

new TemporaryExceptionPlugin(classifier);
```

## Usage

```java
CompletableFuture<Void> future = http.post("/").dispatch(series(),
    on(SUCCESSFUL).call(pass()));
    
try {
    Completion.join(future);
} catch (TemporaryException e) {
    // TODO retry later
}
```

Note: `Completion.join` is a convenience function that get's rid of the outermost `CompletionException` and rethrows
its cause.

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../CONTRIBUTING.md).
