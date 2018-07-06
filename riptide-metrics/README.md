# Riptide: Metrics

[![Gauge](../docs/gauge.jpg)](https://pixabay.com/en/pressure-gauge-meter-water-column-2644531/)

[![Build Status](https://img.shields.io/travis/zalando/riptide/master.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide/master.svg)](https://coveralls.io/r/zalando/riptide)
[![Code Quality](https://img.shields.io/codacy/grade/1fbe3d16ca544c0c8589692632d114de/master.svg)](https://www.codacy.com/app/whiskeysierra/riptide)
[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-metrics.svg)](http://www.javadoc.io/doc/org.zalando/riptide-metrics)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-metrics.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-metrics)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

*Riptide: Metrics* adds metrics support to *Riptide*. It allows to record metrics for all remote requests. 

## Example

```java
Http.builder()
    .plugin(new MetricsPlugin(meterRegistry))
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
    <artifactId>riptide-metrics</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

```java
Http.builder()
    .baseUrl("https://www.example.com")
    .plugin(new MetricsPlugin(meterRegistry)
    .build();
```

It's also possible to specify a custom metrics name with `http.client.requests` being the default
and default tags:

```java
new MetricsPlugin(meterRegistry)
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
