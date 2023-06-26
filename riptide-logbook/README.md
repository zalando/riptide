# Riptide: Logbook

[![Logbook](https://github.com/zalando/logbook/raw/master/docs/logbook.jpg)](https://github.com/zalando/logbook#credits-and-references)

[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-logbook.svg)](http://www.javadoc.io/doc/org.zalando/riptide-logbook)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-logbook.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-logbook)

*Riptide: Logbook* adds [Logbook](https://github.com/zalando/logbook) support to Riptide.

## Example

```java
Http.builder()
    .plugin(new LogbookPlugin(logbook))
    .build();
```

## Features

- Logbook (request and response logging) support

## Dependencies

- Riptide: Core

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-logbook</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

Configuration of Logbook is documented at [zalando/logbook](https://github.com/zalando/logbook#usage).

If configuring it manually is not desired you're encouraged to make use of the
[Logbook-](https://github.com/zalando/logbook#spring-boot-starter) and 
[Riptide Spring Boot Auto Configuration](../riptide-spring-boot-autoconfigure)

The default configuration will produce log message like the following:

```http
Incoming Request: 2d66e4bc-9a0d-11e5-a84c-1f39510f0d6b
GET http://example.org/test HTTP/1.1
Accept: application/json
Host: localhost
Content-Type: text/plain

Hello world!
```

```http
Outgoing Response: 2d66e4bc-9a0d-11e5-a84c-1f39510f0d6b
Duration: 25 ms
HTTP/1.1 200
Content-Type: application/json

{"value":"Hello world!"}
```

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
