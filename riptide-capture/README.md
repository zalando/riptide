# Riptide: Capture

[![Glass of Water](../docs/glass.jpg)](https://pixabay.com/en/glass-water-ice-cubes-drink-cold-1206584/)

[![Build Status](https://img.shields.io/travis/zalando/riptide.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide.svg)](https://coveralls.io/r/zalando/riptide)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/org.zalando/riptide-capture/badge.svg)](http://www.javadoc.io/doc/org.zalando/riptide-capture)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-capture.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-capture)

Put a meaningful, short, plain-language description of what
this project is trying to accomplish and why it matters.
Describe the problem(s) this project solves.
Describe how this software can improve the lives of its audience.

- **Technology stack**: Indicate the technological nature of the software, including primary programming language(s) and whether the software is intended as standalone or as a module in a framework or other ecosystem.
- **Status**:  Alpha, Beta, 1.1, etc. It's OK to write a sentence, too. The goal is to let interested people know where this project is at. This is also a good place to link to the [CHANGELOG](CHANGELOG.md).
- **Links to production or demo instances**
- Describe what sets this apart from related-projects. Linking to another doc or page is OK if this can't be expressed in a sentence or two.

## Example

```java
Capture<Order> capture = Capture.empty();

http.get("/sales-orders/{id}", id).dispatch(series(),
    on(SUCCESSFUL).dispatch(contentType(),
        on(MediaTypes.ORDER).call(Order.class, capture))).get();

return capture.retrieve();
```

## Features

-  **Important** things first

## Dependencies

- Java 8
- Any build tool using Maven Central, or direct download

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

### Synchronous Captures

```java
public Order getOrder(final String id) {
    Capture<Order> capture = Capture.empty();
    
    http.get("/sales-orders/{id}", id).dispatch(series(),
        on(SUCCESSFUL).dispatch(contentType(),
            on(MediaTypes.ORDER).call(Order.class, capture))).get();
    
    return capture.retrieve(); // may throw NoSuchElementException
}
```

**Important**: Block on the future to make sure the capture actually ran.

### Asynchronous Captures

```java
public Future<Order> getOrder(final String id) {
    Capture<Order> capture = Capture.empty();
    
    Future<?> future = http.get("/sales-orders/{id}", id).dispatch(series(),
        on(SUCCESSFUL).dispatch(contentType(),
            on(MediaTypes.ORDER).call(Order.class, capture)));
    
    return capture.adapt(future);
}
```

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../CONTRIBUTING.md).
