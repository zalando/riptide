# Riptide: Timeout

[![Clock Tower](../docs/clock-tower.jpg)](https://pixabay.com/en/water-valley-river-lighthouse-2588151/)

[![Build Status](https://img.shields.io/travis/zalando/riptide/master.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide/master.svg)](https://coveralls.io/r/zalando/riptide)
[![Code Quality](https://img.shields.io/codacy/grade/1fbe3d16ca544c0c8589692632d114de/master.svg)](https://www.codacy.com/app/whiskeysierra/riptide)
[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-timeout.svg)](http://www.javadoc.io/doc/org.zalando/riptide-timeout)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-timeout.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-timeout)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

*Riptide: Timeout* adds timeout support to *Riptide*. It allows to set a *global* timeout to all remote requests
that will be covering any remote communication, socket timeouts and retries. 

## Example

```java
Http.builder()
    .plugin(new TimeoutPlugin(scheduler, 5, SECONDS))
    .build();
```

## Features

- adds global timeouts to Riptide calls

## Dependencies

- Java 8
- Riptide Core

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-timeout</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

```java
Http.builder()
    .plugin(new TimeoutPlugin(scheduler, 5, SECONDS))
    .build();
```

**Make sure**, that you order your plugins correctly when registering, so that the timeout is applied to everything
that you want it to. The `TimeoutPlugin` is usually registered as one of the last plugins.

## Usage

Given the timeout plugin was configured as shown in the last section: A regular call like the following will now be
timed out if it did not finish within 5 seconds:

```java
http.get("/users/me")
    .dispatch(series(),
        on(SUCCESSFUL).call(User.class, this::greet),
        anySeries().call(problemHandling()))
```

**Be aware** that the timeout will also include the runtime of the `greet` method or `problemHandling()` depending
on which one is being executed.

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply open a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
