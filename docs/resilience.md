# Resilience

This document aims to provide a convenient overview of all resilience patterns that are supported by Riptide:

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
riptide:
  clients:
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

Some resilience patterns like retries, queuing, backup requests and asynchronous/remote
fallbacks introduce delays. Configuring connect and socket timeouts in those cases is often not enough.
You need consider maximum number of retries, exponential backoff delays, jitter, etc. An easy way is to set a *global* 
timeout that spans all of the things mentioned before. This is provided by [riptide-timeout](../riptide-timeout):

```yaml
riptide.clients:
  example:
    connect-timeout: 150 milliseconds
    socket-timeout: 100 milliseconds
    timeout: 500 milliseconds
```

## References

- [Uwe Friedrichsen: Patterns of Resilience](https://www.slideshare.net/ufried/patterns-of-resilience)
- [Microsoft: Resiliency patterns](https://docs.microsoft.com/en-us/azure/architecture/patterns/category/resiliency)
- [Patterns of resilience - the untold stories of robust software design by Uwe Friedrichsen](https://www.youtube.com/watch?v=T9MPDmw6MNI)
