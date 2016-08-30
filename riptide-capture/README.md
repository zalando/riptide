# Riptide: Capture

[![Glass of Water](../docs/glass.jpg)](https://pixabay.com/en/glass-water-ice-cubes-drink-cold-1206584/)

[![Build Status](https://img.shields.io/travis/zalando/riptide.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide.svg)](https://coveralls.io/r/zalando/riptide)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/org.zalando/riptide-capture/badge.svg)](http://www.javadoc.io/doc/org.zalando/riptide-capture)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-capture.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-capture)

*Riptide: Capture* adds the possibility to produce synchronous return values with *Riptide*.

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

-  type-safe

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

```java
public Completion<Order> getOrder(final String id) {
    Capture<Order> capture = Capture.empty();
    
    return http.get("/sales-orders/{id}", id)
        .dispatch(series(),
            on(SUCCESSFUL).dispatch(contentType(),
                on(MediaTypes.ORDER).call(Order.class, capture)))
        .thenApply(capture);
}
```

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../CONTRIBUTING.md).
