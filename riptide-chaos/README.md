# Riptide: Chaos

[![Glass of Water](../docs/boat.jpg)](https://pixabay.com/photos/boat-distress-sea-wave-forward-2624054/)

[![Build Status](https://img.shields.io/travis/zalando/riptide.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide.svg)](https://coveralls.io/r/zalando/riptide)
[![Code Quality](https://img.shields.io/codacy/grade/1fbe3d16ca544c0c8589692632d114de/master.svg)](https://www.codacy.com/app/whiskeysierra/riptide)
[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-capture.svg)](http://www.javadoc.io/doc/org.zalando/riptide-capture)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-capture.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-capture)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

*Riptide: Chaos* adds [Chaos](https://en.wikipedia.org/wiki/Chaos_engineering) and [Fault/Failure Injection](https://en.wikipedia.org/wiki/Fault_injection) to Riptide.

## Example

```java
Http.builder()
    .plugin(new ChaosPlugin(
        new LatencyInjection(
            Probability.fixed(0.01), 
            Clock.systemUTC(), 
            Duration.ofSeconds(1))))
    .build();
```

## Features

- Controlled failure injection
- Latency injection
- Error response injection
- Exception injection

## Dependencies

- Java 8
- Riptide: Core

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-chaos</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

### Latency Injection

Prolongs the processing of a response by the given delay. This injection will be
skipped if the response is already delayed.

```java
Http.builder()
    .plugin(new ChaosPlugin(
        new LatencyInjection(
            Probability.fixed(0.01), 
            Clock.systemUTC(), 
            Duration.ofSeconds(1))))
    .build();
```

### Exception Injection

Injects an exception, constructed using one of the given suppliers, and injects it
after the actual response has been received. This injection will be skipped
if any exception already occurred.

```java
Http.builder()
    .plugin(new ChaosPlugin(
        new ExceptionInjection(
            Probability.fixed(0.001), 
            Arrays.asList(
                SocketTimeoutException::new,
                NoRouteToHostException::new)))
    .build();
```

### Error Response Injection

Injects an error response, using one of the given status codes, and injects it
after the actual has been received. This injection will be skipped if any
error response (4xx or 5xx) already occurred.

```java
Http.builder()
    .plugin(new ChaosPlugin(
        new ErrorResponseInjection(
            Probability.fixed(0.001), 
            Arrays.asList(
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.SERVICE_UNAVAILABLE))))
    .build();
```

### Composing injections

If you want to enabled multiple different failure injections at the same time you
can use the composite:

```java
new ChaosPlugin(composite(
        new LatencyInjection(
            Probability.fixed(0.01), 
            Clock.systemUTC(), 
            Duration.ofSeconds(1))),
        new ExceptionInjection(
            Probability.fixed(0.001), 
            Arrays.asList(
                SocketTimeoutException::new,
                NoRouteToHostException::new)))
```

Please note that since both injections evaluate their probability independently
the chance that both happen at the same time is the product of both, e.g. 0.00001 or 0.001%.

### Probability

The built-in default implementation for `Probability` is a fixed one using a random
number generator and a given probability `[0..1)`.

A more sophisticated implementation could use some shared configuration with support
for updating configuration at runtime to control the probability of certain failure
injections in a more dynamic, controlled way, e.g. during a fire drill.

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
