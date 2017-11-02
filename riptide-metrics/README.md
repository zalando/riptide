# Riptide: Metrics

[![Gauge](../docs/gauge.jpg)](https://pixabay.com/en/pressure-gauge-meter-water-column-2644531/)

[![Build Status](https://img.shields.io/travis/zalando/riptide/master.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide/master.svg)](https://coveralls.io/r/zalando/riptide)
[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-metrics.svg)](http://www.javadoc.io/doc/org.zalando/riptide-metrics)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-metrics.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-metrics)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

*Riptide: Metrics* adds metrics support to *Riptide*. It allows to measure
 It allows to set a *global* timeout to all remote requests
that will be covering any remote communication, socket timeouts and retries. 

## Example

```java
Http.builder()
    .plugin(new MetricsPlugin(gaugeService, nameGenerator))
    .build();
```

## Features

- adds global timeouts to Riptide calls

## Dependencies

- Java 8
- Riptide Core
- Spring Boot Actuator

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
    .plugin(new MetricsPlugin(gaugeService, (arguments, response) ->
            String.format("request.%s.%s", 
                arguments.getMethod(), 
                arguments.getRequestUri().getHost()))
    .build();
```

The first parameter is a [`GaugeService`](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/actuate/metrics/GaugeService.html)
(usually provided by the Spring Boot Actuator)
and the second one is a `MetricsNameGenerator` that allows you to specify the metrics name under which metrics are
submitted. It gives you full access to all request arguments as well as the complete response:

```java
public interface MetricsNameGenerator {

    String generate(RequestArguments arguments, ClientHttpResponse response);

}
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

If you're using the [Spring Boot Metrics Endpoint](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-metrics.html)
then you should be able to see the corresponding metrics in the `/metrics` endpoint:

```json
{
  "request.GET.www.example.com.count": 0,
  "request.GET.www.example.com.fifteenMinuteRate": 0.0,
  "request.GET.www.example.com.fiveMinuteRate": 0.0,
  "request.GET.www.example.com.meanRate": 0.0,
  "request.GET.www.example.com.oneMinuteRate": 0.0,
  "request.GET.www.example.com.snapshot.75thPercentile": 0,
  "request.GET.www.example.com.snapshot.95thPercentile": 0,
  "request.GET.www.example.com.snapshot.98thPercentile": 0,
  "request.GET.www.example.com.snapshot.999thPercentile": 0,
  "request.GET.www.example.com.snapshot.99thPercentile": 0,
  "request.GET.www.example.com.snapshot.max": 0,
  "request.GET.www.example.com.snapshot.mean": 0,
  "request.GET.www.example.com.snapshot.median": 0,
  "request.GET.www.example.com.snapshot.min": 0,
  "request.GET.www.example.com.snapshot.stdDev": 0
}
```

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply open a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../CONTRIBUTING.md).
