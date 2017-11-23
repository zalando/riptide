# Resilience

This document aims to provide a convenient overview of all resilience patterns that are supported by Riptide.
A complete example configuration can be found [here](https://github.com/zalando/riptide/tree/master/riptide-spring-boot-starter#configuration).

## Bulkheads

> Isolate elements of an application into pools so that if one fails, the others will continue to function.
> This pattern is named Bulkhead because it resembles the sectioned partitions of a ship's hull. If the hull of a ship is compromised, only the damaged section fills with water, which prevents the ship from sinking.
> 
> [Microsoft: Bulkhead pattern](https://docs.microsoft.com/en-us/azure/architecture/patterns/bulkhead)

Riptide supports the bulkhead pattern by supporting isolated, fixed-size thread pools and bounded work queues per 
client/instance. 

```yaml
riptide.clients:
  example:
    thread-pool:
      min-size: 4
      max-size: 16
      keep-alive: 1 minute
      queue-size: 0
```

- [Stack Overflow: What is bulk head pattern](https://stackoverflow.com/a/30685644/232539)

## Transient Faults

> [..] transient faults, such as slow network connections, timeouts, or the resources being overcommitted or temporarily unavailable [..]
> 
> [Microsoft: Retry pattern](https://docs.microsoft.com/en-us/azure/architecture/patterns/retry)

The [riptide-faults](../riptide-faults) module provides a `TransientFaultPlugin` that detects transient faults:

```java
Http.builder()
    .plugin(new TransientFaultPlugin())
```

```yaml
riptide.clients:
  example:
    detect-transient-faults: true
```

## Retries

> Enable an application to handle transient failures when it tries to connect to a service or network resource, by transparently retrying a failed operation. This can improve the stability of the application.
>
> [Microsoft: Retry pattern](https://docs.microsoft.com/en-us/azure/architecture/patterns/retry)

Provided by [riptide-failsafe](../riptide-failsafe)

```yaml
riptide.clients:
  example:
    retry:
      fixed-delay: 50 milliseconds
      max-retries: 5
      max-duration: 2 second
      jitter: 25 milliseconds
```

## Circuit Breaker

> Handle faults that might take a variable amount of time to recover from, when connecting to a remote service or resource. This can improve the stability and resiliency of an application.
>
> [Microsoft: Circuit Breaker pattern](https://docs.microsoft.com/en-us/azure/architecture/patterns/circuit-breaker)

Provided by [riptide-failsafe](../riptide-failsafe)

```yaml
riptide.clients:
  example:
    circuit-breaker:
      failure-threshold: 3 out of 5
      delay: 30 seconds
      success-threshold: 5 out of 5
```

## Backup Request

> A simple way to
  curb latency variability is to issue the
  same request to multiple replicas and
  use the results from whichever replica
  responds first. [..] defer sending
  a secondary request until the first
  request has been outstanding for more
  than the 95th-percentile expected latency
  for this class of requests
> 
> [Jeffrey Dean and Luiz André Barroso: The Tail at Scale](http://www.cs.duke.edu/courses/cps296.4/fall13/838-CloudPapers/dean_longtail.pdf)

Provided by [riptide-backup](../riptide-backup)

```yaml
riptide.clients:
  example:
    backup-request:
      delay: 75 milliseconds
```

## Fallbacks  

Provided by:
- riptide-core
- `CompletableFuture.exceptionally(Function)`
- [faux-pas](https://github.com/zalando/faux-pas#completablefutures-exceptionally)

```java
Capture<User> capture = Capture.empty();

http.get("/me")
    .dispatch(series(),
        on(SUCCESSFUL).call(User.class, capture),
        on(CLIENT_ERROR).dispatch(status(),
            on(UNAUTHORIZED).call(() -> 
                capture.accept(new Anonymous()))))
    .thenApply(capture)
    .exceptionally(e -> new Unknown());
```

## Timeouts

Some resilience patterns like retries, queuing, backup requests and fallbacks introduce delays. Configuring connect and
socket timeouts in those cases is often not enough. You need consider maximum number of retries, exponential backoff
delays, jitter, etc. An easy way is to set a *global* timeout that spans all of the things mentioned before. This 
feature is provided by [riptide-timeout](../riptide-timeout).

Given the following sample configuration:

```yaml
riptide.clients:
  example:
    connect-timeout: 50 milliseconds
    socket-timeout: 25 milliseconds
    retry:
      fixed-delay: 30 milliseconds
      max-retries: 5
      jitter: 15 milliseconds
    backup-request:
      delay: 75 milliseconds
    timeout: 500 milliseconds
```

Based on the absolute worst-case scenario, one would need to expect:

[`50ms + 25ms + 75ms + 5 × (50ms + 25ms + 30ms + 15ms) = 750ms`](https://www.wolframalpha.com/input/?i=50ms+%2B+25ms+%2B+75ms+%2B+5+%C3%97+(50ms+%2B+25ms+%2B+30ms+%2B+15ms))

This calculation does not factor in delays because of queuing. Neither does it consider the processing time to process
the response, e.g. deserialization.

Let's assume there is a budget of 500 ms that can be spend on this remote call. The calculation above shows
that the worst-case scenario would extend way beyond that given budget. But it's very unlikely that you'll hit the
absolute worst case. If a connection can be established it won't take exactly 50 ms every time. The jitter
will vary in how long the delay will be, somewhere between 15 and 45 ms and 30 ms on average.
For example, let's assume you connect within 1 ms, but hit the socket timeout 5 times in a row:

[`1ms + 25ms + 75ms + 5 × (1ms + 25ms + 30ms) = 381ms`](https://www.wolframalpha.com/input/?i=1ms+%2B+25ms+%2B+75ms+%2B+5+%C3%97+(1ms+%2B+25ms+%2B+30ms))

Your budget allows for this, so there is no reason not to use it. Setting a timeout allows you to maximize your budget and minimize the risk of exceeding it.

## References

- [Uwe Friedrichsen: Patterns of Resilience](https://www.slideshare.net/ufried/patterns-of-resilience)
- [Microsoft: Resiliency patterns](https://docs.microsoft.com/en-us/azure/architecture/patterns/category/resiliency)
- [Patterns of resilience - the untold stories of robust software design by Uwe Friedrichsen](https://www.youtube.com/watch?v=T9MPDmw6MNI)
