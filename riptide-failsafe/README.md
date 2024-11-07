# Riptide: Failsafe

[![Valves](../docs/valves.jpg)](https://pixabay.com/en/wheel-valve-heating-line-turn-2137043/)

[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-failsafe.svg)](http://www.javadoc.io/doc/org.zalando/riptide-failsafe)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-failsafe.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-failsafe)

*Riptide: Failsafe* adds [Failsafe](https://github.com/jhalterman/failsafe) support to *Riptide*. It offers retries
and a circuit breaker to every remote call.

## Example

```java
Http.builder().requestFactory(new HttpComponentsClientHttpRequestFactory())
    .plugin(new FailsafePlugin()
        .withPolicy(circuitBreaker)
        .withPolicy(new RetryRequestPolicy(retryPolicy)))
    .build();
```

## Features

- seamlessly integrates Riptide with Failsafe

## Dependencies

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
Http.builder().requestFactory(new HttpComponentsClientHttpRequestFactory())
    .plugin(new FailsafePlugin()
        .withPolicy(
            new RetryRequestPolicy(
                RetryPolicy.<ClientHttpResponse>builder()
                    .withDelay(Duration.ofMillis(25))
                    .withDelayFn(new RetryAfterDelayFunction(clock))
                    .withMaxRetries(4)
                    .build())
                .withListener(myRetryListener))
        .withPolicy(
            CircuitBreaker.<ClientHttpResponse>builder()
                .withFailureThreshold(3, 10)
                .withSuccessThreshold(5)
                .withDelay(Duration.ofMinutes(1))
                .build()))
    .build();
```

Please visit the [Failsafe readme](https://github.com/jhalterman/failsafe#readme) in order to see possible configurations. 

### Retries

**Beware** when using `retryOn` to retry conditionally on certain exception types.
You'll need to register `RetryException` in order for the `retry()` route to work:

```java
RetryPolicy.<ClientHttpResponse>builder()
    .handle(SocketTimeoutException.class)
    .handle(RetryException.class)
    .build();
```

By default, you can use RetryException in your routes to retry the request:

```java
retryClient.get()
    .dispatch(
        series(), on(CLIENT_ERROR).call(
            response -> {
                if (specificCondition(response)) {
                    throw new RetryException(response); // we will retry this one
                }  else {
                    throw new AnyOtherException(response); // we wont retry this one
                }  
            }
        )
    ).join()
```

Failsafe supports dynamically computed delays using a custom function.

Riptide: Failsafe offers implementations that understand:
- [`Retry-After` (RFC 7231, section 7.1.3)](https://tools.ietf.org/html/rfc7231#section-7.1.3)
- [`X-RateLimit-Reset` (RESTful API Guidelines)](https://opensource.zalando.com/restful-api-guidelines/#153)

```java
Http.builder().requestFactory(new HttpComponentsClientHttpRequestFactory())
    .plugin(new FailsafePlugin()
        .withPolicy(RetryPolicy.<ClientHttpResponse>builder()
            .withDelayFn(new CompositeDelayFunction<>(Arrays.asList(
                new RetryAfterDelayFunction(clock),
                new RateLimitResetDelayFunction(clock)
            )))
            .withMaxDuration(Duration.ofSeconds(5))
            .build()))
    .build();
```

:warning: Make sure you you specify a **max duration** otherwise any value coming from a server that is further ahead in the future will make your retry block practically forever.

Make sure you **check out 
[zalando/failsafe-actuator](https://github.com/zalando/failsafe-actuator)** for a seamless integration of
Failsafe and Spring Boot.

### Timeout policy

You can use `org.springframework.http.client.ClientHttpRequestFactory` configuration to set up proper
connection timeout, socket timeout and connection time to live.
In addition you can use `FailsafePlugin` with `dev.failsafe.Timeout` policy to control the entire duration
from sending the request to processing the response. See the use cases in the `FailsafePluginTimeoutTest` test.

Configuration example:
```java
 Http.builder().requestFactory(new HttpComponentsClientHttpRequestFactory())
                .plugin(new FailsafePlugin()
                        .withPolicy(Timeout.of(Duration.ofSeconds(5))))
                .build();
```


### Backup Requests

The `BackupRequest` policy implements the [*backup request*][abstract] pattern, also known as [*hedged requests*][article]:

```java
Http.builder().requestFactory(new HttpComponentsClientHttpRequestFactory())
    .plugin(new FailsafePlugin()
        .withPolicy(new BackupRequest(1, SECONDS)))
    .build();
```

### Custom executor

The `withExecutor` method allows to specify a custom `ExecutorService` being used to perform asynchronous executions and listen for callbacks:

```java
Http.builder().requestFactory(new HttpComponentsClientHttpRequestFactory())
    .plugin(new FailsafePlugin()
        .withPolicy(
            CircuitBreaker.<ClientHttpResponse>builder()
                .withFailureThreshold(3, 10)
                .withSuccessThreshold(5)
                .withDelay(Duration.ofMinutes(1))
                .build())
        .withExecutor(Executors.newFixedThreadPool(2)))
    .build();
```

If no executor is specified, the default executor configured by `Failsafe` is used. See [Failsafe DelegatingScheduler class](https://github.com/failsafe-lib/failsafe/blob/master/core/src/main/java/dev/failsafe/internal/util/DelegatingScheduler.java#L111), 
and also [Failsafe documentation](https://failsafe.dev/async-execution/#executorservice-configuration) for more information.

**Beware** when specifying a custom `ExecutorService`: 
1. The `ExecutorService` should have a core pool size or parallelism of at least 2 in order for [timeouts](https://github.com/failsafe-lib/failsafe/blob/master/core/src/main/java/dev/failsafe/Timeout.java) to work
2. In general, it is not recommended to specify the same `ExecutorService` for multiple `Http` clients 

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
Http.builder().requestFactory(new HttpComponentsClientHttpRequestFactory())
    .plugin(new FailsafePlugin()
        .withPolicy(retryPolicy)
        .withDecorator(new CustomIdempotentMethodDetector()))
    .build();
```

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply open a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).

## Credits and references

- [Jeffrey Dean: Achieving Rapid Response Times in Large Online Services][abstract]
- [Jeffrey Dean and Luiz André Barroso: The Tail at Scale][article]
- [Uwe Friedrichsen: Patterns of Resilience - Fan Out, Quickest Reply](https://www.slideshare.net/ufried/patterns-of-resilience/61)

[abstract]: https://research.google.com/people/jeff/latency.html
[article]: http://www.cs.duke.edu/courses/cps296.4/fall13/838-CloudPapers/dean_longtail.pdf
