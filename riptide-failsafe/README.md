# Riptide: Failsafe

[![Valves](../docs/valves.jpg)](https://pixabay.com/en/wheel-valve-heating-line-turn-2137043/)

[![Build Status](https://img.shields.io/travis/zalando/riptide/master.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide/master.svg)](https://coveralls.io/r/zalando/riptide)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/org.zalando/riptide-failsafe/badge.svg)](http://www.javadoc.io/doc/org.zalando/riptide-failsafe)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-failsafe.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-failsafe)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

*Riptide: Failsafe* adds [Failsafe](https://github.com/jhalterman/failsafe) support to *Riptide*. It offers retries
and a circuit breaker to every remote call.

## Example

```java
Http.builder()
    .plugin(new FailsafePlugin(scheduler)
            .withRetryPolicy(retryPolicy)
            .withCircuitBreaker(circuitBreaker))
    .build();
```

## Features

-  seamlessly integrates Riptide with Failsafe

## Dependencies

- Java 8
- Riptide Core
- Failsafe

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-failsafe</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

The failsafe plugin will not perform retries nor apply circuit breakers unless they were explicitly configured:

```java
Http.builder()
    .plugin(new FailsafePlugin(Executors.newSingleThreadScheduledExecutor())
            .withRetryPolicy(new RetryPolicy()
                    .retryOn(SocketTimeoutException.class)
                    .withDelay(25, TimeUnit.MILLISECONDS)
                    .withMaxRetries(4))
            .withCircuitBreaker(new CircuitBreaker()
                    .withFailureThreshold(3, 10)
                    .withSuccessThreshold(5)
                    .withDelay(1, TimeUnit.MINUTES)))
    .build();
```

Please visit the [Failsafe readme](https://github.com/jhalterman/failsafe#readme) in order to see which configuration
is possible.

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../CONTRIBUTING.md).
