# Riptide: Micrometer

[![Gauge](../docs/gauge.jpg)](https://pixabay.com/en/pressure-gauge-meter-water-column-2644531/)

[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-micrometer.svg)](http://www.javadoc.io/doc/org.zalando/riptide-micrometer)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-micrometer.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-micrometer)

*Riptide: Micrometer* adds metrics support to *Riptide*. It allows to record metrics for all remote requests and responses. 

## Example

```java
Http.builder()
    .plugin(new MicrometerPlugin(meterRegistry))
    .build();
```

## Features

- adds request metrics to Riptide

## Dependencies

- Java 8
- Riptide Core
- [Micrometer](https://micrometer.io/)

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-micrometer</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

```java
Http.builder()
    .baseUrl("https://www.example.com")
    .plugin(new MicrometerPlugin(meterRegistry)
    .build();
```

It's also possible to specify a custom metrics name with `http.client.requests` being the default
and default tags:

```java
new MicrometerPlugin(meterRegistry)
    .withMetricName("http.outgoing-requests")
    .withDefaultTags(Tag.of("aws.region", "eu-central-1"))
```

## Usage

The plugin will measure network communication but exclude any logic that is part of the local routing tree, i.e. `greet`
in the following example:

```java
http.get("/users/me")
    .dispatch(series(),
        on(SUCCESSFUL).call(User.class, this::greet),
        anySeries().call(problemHandling()))
```

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply open a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
