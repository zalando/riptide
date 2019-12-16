# Riptide: Compatibility

[![Toy Car](../docs/toy-car.jpg)](https://pixabay.com/photos/miniature-car-model-toy-automobile-1802333/)

[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-compatibility.svg)](http://www.javadoc.io/doc/org.zalando/riptide-compatibility)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-compatibility.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-compatibility)

*Riptide: Compatibility* adds a Spring Web Client compatibility layer on top of Riptide.

## Example

```java
User user = http.getForObject("/user/{id}", User.class, 1);
```

## Features

- Compatibility adapters for
  - [RestOperations (**RestTemplate** API)](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/RestOperations.html)
  - [AsyncRestOperations (**AsyncRestTemplate** API)](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/AsyncRestOperations.html)

## Dependencies

- Java 8
- Riptide: Core
- Riptide: Capture
- Riptide: Problem

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-compatibility</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

```java
RestOperations http = new HttpOperations(Http.builder()
        // ...
        .build());
```

```java
AsyncRestOperations http = new AsyncHttpOperations(Http.builder()
        // ...
        .build());
```

### Default Routing Tree

The default routing tree can be overridden:

```java
new HttpOperations(http)
        .withDefaultRoutingTree(tree);
```

If not specified the routing tree will default to:

```java
new HttpOperations(http)
        .withDefaultRoutingTree(
            dispatch(series(),
                anySeries().call(problemHandling())));
```
That gives you the following, effective routing tree:

```
Series?
├── 2xx: capture/pass
└── any: Content-Type?
    ├── application/problem+json: throw
    ├── application/x.problem+json: throw
    ├── application/x-problem+json: throw
    └── any: unsupported response
```

## Usage

```java
ResponseEntity<User> response = http.getForEntity("/users/{id}", User.class, 1);
```

Any operation within [RestOperations](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/RestOperations.html)
and [AsyncRestOperations](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/AsyncRestOperations.html)
are supported.

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
