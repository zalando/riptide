# Riptide: Failsafe

[![Valves](../docs/valves.jpg)](https://pixabay.com/en/wheel-valve-heating-line-turn-2137043/)

[![Build Status](https://img.shields.io/travis/zalando/riptide/master.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide/master.svg)](https://coveralls.io/r/zalando/riptide)
[![Code Quality](https://img.shields.io/codacy/grade/1fbe3d16ca544c0c8589692632d114de/master.svg)](https://www.codacy.com/app/whiskeysierra/riptide)
[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-failsafe.svg)](http://www.javadoc.io/doc/org.zalando/riptide-failsafe)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-failsafe.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-failsafe)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

*Riptide: Failsafe* adds [Failsafe](https://github.com/jhalterman/failsafe) support to *Riptide*. It offers retries
and a circuit breaker to every remote call.

## Example

```java
Http.builder()
    .plugin(new FailsafePlugin(ImmutableList.of(circuitBreaker, retryPolicy), scheduler))
    .build();
```

## Features

- seamlessly integrates Riptide with Failsafe

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
    .plugin(new FailsafePlugin(
            ImmutableList.of(
                   new RetryPolicy<ClientHttpResponse>()
                           .withDelay(Duration.ofMillis(25))
                           .withDelay(new RetryAfterDelayFunction(clock))
                           .withMaxRetries(4),
                   new CircuitBreaker<ClientHttpResponse>()
                           .withFailureThreshold(3, 10)
                           .withSuccessThreshold(5)
                           .withDelay(Duration.ofMinutes(1))
            ),
            Executors.newScheduledThreadPool(20))
            .withListener(myRetryListener))
    .build();
```

Please visit the [Failsafe readme](https://github.com/jhalterman/failsafe#readme) in order to see possible
configurations. 

**Beware** when using `retryOn` to retry conditionally on certain exception types.
You'll need to register `RetryException` in order for the `retry()` route to work:

```java
new RetryPolicy<ClientHttpResponse>()
    .handle(SocketTimeoutException.class)
    .handle(RetryException.class);
```

Failsafe supports dynamically computed delays using a custom function.

Riptide: Failsafe offers implementations that understand:
- [`Retry-After` (RFC 7231, section 7.1.3)](https://tools.ietf.org/html/rfc7231#section-7.1.3)
- [`X-RateLimit-Reset` (RESTful API Guidelines)](https://opensource.zalando.com/restful-api-guidelines/#153)

```java
Http.builder()
    .plugin(new FailsafePlugin(
            ImmutableList.of(new RetryPolicy<ClientHttpResponse>()
                     .withDelay(Duration.ofMillis(25))
                     .withDelay(new CompositeDelayFunction<>(Arrays.asList(
                             new RetryAfterDelayFunction(clock),
                             new RateLimitResetDelayFunction(clock)
                     )))
                     .withMaxDuration(Duration.ofSeconds(5))),
            Executors.newScheduledThreadPool(20)))
    .build();
```

:warning: Make sure you you specify a **max duration** otherwise any value coming from a server
that is further ahead in the future will make your retry block practically forever.

Make sure you **check out 
[zalando/failsafe-actuator](https://github.com/zalando/failsafe-actuator)** for a seamless integration of
Failsafe and Spring Boot.

## Usage

Given the failsafe plugin was configured as shown in the last section: A regular call like the following will now be
retried up to 4 times if the server did not respond within the socket timeout.

```java
http.get("/users/me")
    .dispatch(series(),
        on(SUCCESSFUL).call(User.class, this::greet),
        anySeries().call(problemHandling()))
```

Handling certain technical issues automatically, like socket timeouts, is quite useful.
But there might be cases where the server did respond, but the response indicates something that is worth
retrying, e.g. a `409 Conflict` or a `503 Service Unavailable`. Use the predefined `retry` route that comes with the
failsafe plugin:

```java
http.get("/users/me")
    .dispatch(series(),
        on(SUCCESSFUL).call(User.class, this::greet),
        on(CLIENT_ERROR).dispatch(status(),
            on(CONFLICT).call(retry())),
        on(SERVER_ERROR).dispatch(status(),
            on(SERVICE_UNAVAILABLE).call(retry())),
        anySeries().call(problemHandling()))
```

### Safe and Idempotent methods

Only [safe](https://tools.ietf.org/html/rfc7231#section-4.2.1) and [idempontent](https://tools.ietf.org/html/rfc7231#section-4.2.2)
methods are retried by default. The following request methods can be detected:

- Standard HTTP method
- [HTTP method override](https://opensocial.github.io/spec/2.5.1/Core-API-Server.xml#rfc.section.2.1.1.19)
- [Conditional Requests](https://tools.ietf.org/html/rfc7232)
- [`Idempotency-Key` header](https://stripe.com/docs/api#idempotent_requests)

You also have the option to declare any request to be `idempotent` by setting the respective request attribute. This is
useful in situation where none of the options are above would detect it but based on the contract of the API you may know
that a certain operation is in fact idempotent.

```java
http.post("/subscriptions/{id}/cursors", subscriptionId)
    .attribute(MethodDetector.IDEMPOTENT, true)
    .header("X-Nakadi-StreamId", streamId)
    .body(cursors)
    .dispatch(series(),
        on(SUCCESSFUL).call(pass()),
        anySeries().call(problemHandling()))
```

In case those options are insufficient you may specify your own method detector:

```java
Http.builder()
    .plugin(new FailsafePlugin(ImmutableList.of(retryPolicy), scheduler)
        .withIdempontentMethodDetector(new CustomIdempotentMethodDetector()))
    .build();
```

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply open a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
