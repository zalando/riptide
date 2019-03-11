# Riptide: Faults

[![Light bulb](../docs/bulb.jpg)](https://pixabay.com/en/electric-light-bulb-wire-rain-2616487/)

[![Build Status](https://img.shields.io/travis/zalando/riptide.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide.svg)](https://coveralls.io/r/zalando/riptide)
[![Code Quality](https://img.shields.io/codacy/grade/1fbe3d16ca544c0c8589692632d114de/master.svg)](https://www.codacy.com/app/whiskeysierra/riptide)
[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-faults.svg)](http://www.javadoc.io/doc/org.zalando/riptide-faults)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-faults.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-faults)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

> **[Transient fault](https://en.wikipedia.org/wiki/Fault_(power_engineering)#Transient_fault)**: a fault that is no longer present if power is disconnected for a short time and then restored

*Riptide: Faults* helps to classify exceptions into *persistent* and *transient* faults.

## Features

- exception classification
- easier exception handling, e.g. for retries
- reusable

## Dependencies

- Java 8
- Riptide: Core

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-faults</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

```java
Http.builder()
    .plugin(new TransientFaultPlugin())
    .build();
```

By default any `IOException` is classified as transient **unless** it's one of the following:

- [`UnknownHostException`](https://docs.oracle.com/javase/8/docs/api/java/net/UnknownHostException.html)
- [`SSLException`](https://docs.oracle.com/javase/8/docs/api/javax/net/ssl/SSLException.html)  
  unless the message indicates a connection issue
- [`MalformedURLException`](https://docs.oracle.com/javase/8/docs/api/java/net/MalformedURLException.html)

In order to change this you can pass in a custom `FaultClassifier`:

```java
FaultClassifier classifier = throwable -> 
        throwable instanceof UnknownHostException ?;
        new TransientFaultException(throwable) :
        throwable;

new TransientFaultPlugin(classifier);
```

But it's a more common use case to augment the defaults, i.e. add some more predicates:

```java
FaultClassifier = new DefaultFaultClassifier()
        .include(UnknownHostException.class::isInstance)
        .exclude(InterruptedIOException.class::isInstance);

new TransientFaultPlugin(predicates);
```

The `DefaultFaultClassifier` is inspecting the whole causal chain of an exception. This behavior can be
changed by specifying a different `ClassificationStrategy`:

- `CausalChainClassificationStrategy`: The exception itself including all causes
- `RootCauseClassificationStrategy`: The exception's root cause
- `SelfClassificationStrategy`: The exception itself

## Usage

```java
CompletableFuture<ClientHttpResponse> future = http.post("/")
        .dispatch(series(),
            on(SUCCESSFUL).call(pass()));
    
try {
    future.join();
} catch (CompletionException e) {
    boolean isTransient = e.getCause() instanceof TransientFaultException;
    // TODO retry later on transient
}
```

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
