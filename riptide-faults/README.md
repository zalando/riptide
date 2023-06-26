# Riptide: Faults

[![Light bulb](../docs/bulb.jpg)](https://pixabay.com/en/electric-light-bulb-wire-rain-2616487/)

[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-faults.svg)](http://www.javadoc.io/doc/org.zalando/riptide-faults)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-faults.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-faults)

> **[Transient fault](https://en.wikipedia.org/wiki/Fault_(power_engineering)#Transient_fault)**: a fault that is no longer present if power is disconnected for a short time and then restored

*Riptide: Faults* helps to classify exceptions into *persistent* and *transient* faults.

## Features

- exception classification
- easier exception handling, e.g. for retries
- reusable

## Dependencies

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

A very common usage is to register the `transientFaults` predicate with a *Failsafe* retry policy:

```java
new RetryPolicy<>()
    .handleIf(TransientFaults.transientFaults());
```

### Defaults

The `TransientFaults` predicates define defaults for transient fault classification. Apart from `transientFaults` there are the more fine-grained `transientConnectionFaults` and `transientSocketFaults`:

#### Connection faults

Transient connections faults happen before any request is either made, send or received by the server. Those faults can safely be retried, regardless of the actual request being made, since the server never had a chance to handle it:

```java
new RetryRequestPolicy(
        new RetryPolicy<>()
            .handleIf(TransientFaults.transientConnectionFaults()))
    .withPredicate(alwaysTrue());
```

The default considers the following exceptions to be transient connection faults:
- [`ConnectionException`](https://docs.oracle.com/javase/8/docs/api/java/net/ConnectionException.html)
- [`MalformedURLException`](https://docs.oracle.com/javase/8/docs/api/java/net/MalformedURLException.html)
- [`NoRouteToHostException`](https://docs.oracle.com/javase/8/docs/api/java/net/NoRouteToHostException.html)
- [`UnknownHostException`](https://docs.oracle.com/javase/8/docs/api/java/net/UnknownHostException.html)

#### Socket faults

Transient socket faults happen after a request was (at least potentially) received by a server. Those faults should only be retried if the request is idempotent:

```java
new RetryRequestPolicy(
        new RetryPolicy<>()
            .handleIf(TransientFaults.transientSocketFaults()))
    .withPredicate(new IdempotencyPredicate()); // the default
```

The default considers the following exceptions to be transient socket faults:
- [`IOException`](https://docs.oracle.com/javase/8/docs/api/java/io/IOException.html)
- **unless** it's one of the following:
    - [`SSLException`](https://docs.oracle.com/javase/8/docs/api/javax/net/ssl/SSLException.html) except when the message indicates a connection issue

### Customization

In order to customize this you can use any of the existing `TransientFaults.Rules` as a blueprint and build customized predicates:

```java
Predicate<Throwable> predicate = TransientFaults.combine(
    ClassificationStrategy.causalChain(),
    TransientFaults.Rules.transientFaultRules()
        .exclude(SomeSpecialIOException.class::isInstance)
);
```

The default predicates inspect the whole causal chain of an exception. This behavior can be changed by specifying a different `ClassificationStrategy`:

- `causalChain()`: The exception itself including all causes
- `rootCause()`: The exception's root cause
- `self()`: The exception itself

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
