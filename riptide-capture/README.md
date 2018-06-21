# Riptide: Capture

[![Glass of Water](../docs/glass.jpg)](https://pixabay.com/en/glass-water-ice-cubes-drink-cold-1206584/)

[![Build Status](https://img.shields.io/travis/zalando/riptide.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide.svg)](https://coveralls.io/r/zalando/riptide)
[![Code Quality](https://img.shields.io/codacy/grade/1fbe3d16ca544c0c8589692632d114de/master.svg)](https://www.codacy.com/app/whiskeysierra/riptide)
[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-capture.svg)](http://www.javadoc.io/doc/org.zalando/riptide-capture)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-capture.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-capture)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

*Riptide: Capture* adds the possibility to produce meaningful return values with *Riptide*.

## Example

```java
Capture<Order> capture = Capture.empty();

Order order = http.get("/sales-orders/{id}", id)
    .dispatch(series(),
        on(SUCCESSFUL).dispatch(contentType(),
            on(MediaTypes.ORDER).call(Order.class, capture)))
    .thenApply(capture).join();
```

## Features

- produce return values based on Riptide's asynchronous API
- type-safe

## Dependencies

- Java 8
- Riptide: Core

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-capture</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Usage

### Asynchronous return values

```java
public CompletableFuture<Order> getOrder(final String id) {
    Capture<Order> capture = Capture.empty();
    
    return http.get("/sales-orders/{id}", id)
        .dispatch(series(),
            on(SUCCESSFUL).dispatch(contentType(),
                on(MediaTypes.ORDER).call(Order.class, capture)))
        .thenApply(capture);
}
```

### Synchronous return values

```java
public Order getOrder(final String id) {
    Capture<Order> capture = Capture.empty();
    
    CompletableFuture<Order> future = http.get("/sales-orders/{id}", id)
        .dispatch(series(),
            on(SUCCESSFUL).dispatch(contentType(),
                on(MediaTypes.ORDER).call(Order.class, capture)))
        .thenApply(capture);
    
    return Completion.join(future);
}
```

`Completion.join(CompletableFuture)` unwraps any `CompletionException` and sneakily re-throws the cause.

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
