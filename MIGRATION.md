# Riptide 5.0 Migration Guide

## Before you start

**Riptide 5** requires Java 17 or higher.
**Riptide 5** requires Spring 7 (Spring Boot 4) or higher.

## Spring Framework 7 / Spring Boot 4

Riptide 5.0 migrates to Spring Framework 7 and Spring Boot 4 including several breaking changes that affect Riptide users.

### Jackson 3.x Migration

Spring Boot 4 migrates from Jackson 2.x to Jackson 3.x including a major package switch from `com.fasterxml.jackson` to `tools.jackson`.

**Key Changes:**

- Jackson group id changed from `com.fasterxml.jackson.*` to `tools.jackson.*` (for core modules)
- Jackson Annotations remains at `com.fasterxml.jackson.core:jackson-annotations` but switches to version 2.20+
- If you use Jackson directly in your code, you'll need to update imports:
  - `com.fasterxml.jackson.databind.*` → `tools.jackson.databind.*`
  - `com.fasterxml.jackson.core.*` → `tools.jackson.core.*`

**Migration Steps:**

1. Update your Jackson dependencies to Jackson 3.x compatible versions
2. Replace all `com.fasterxml.jackson.databind` imports with `tools.jackson.databind`
3. Replace all `com.fasterxml.jackson.core` imports with `tools.jackson.core`
4. Leave `com.fasterxml.jackson.core.jackson-annotations` imports unchanged

### HTTP Message Converters

Spring Boot 4 renames the primary Jackson HTTP message converter:

**Before (Riptide 4.x):**
```java
Http.builder()
    .converter(new MappingJackson2HttpMessageConverter())
    .build();
```

**After (Riptide 5.x):**
```java
Http.builder()
    .converter(new JacksonJsonHttpMessageConverter())
    .build();
```

### Removed Jackson Modules

The following Jackson modules are no longer automatically included:

- `jackson-module-parameter-names`
- `jackson-datatype-jdk8`
- `jackson-datatype-problem`

### Problem Library Changes

Riptide 5.0 removes the dependency on Zalando's `problem` library in favor of Spring's built-in `ProblemDetail` support (RFC 9457):

**Before (Riptide 4.x):**
```java
import org.zalando.problem.Problem;
import org.zalando.problem.Exceptional;

try {
    http.post("/").dispatch(series(),
        on(SUCCESSFUL).call(pass()),
        anySeries().call(problemHandling()))
        .join();
} catch (CompletionException e) {
    Problem problem = (Problem) e.getCause();
    // handle problem
}
```

**After (Riptide 5.x):**
```java
import org.springframework.http.ProblemDetail;
import org.zalando.riptide.problem.ProblemResponseException;

try {
    http.post("/").dispatch(series(),
        on(SUCCESSFUL).call(pass()),
        anySeries().call(problemHandling()))
        .join();
} catch (CompletionException e) {
    if (e.getCause() instanceof ProblemResponseException) {
        ProblemDetail problem = ((ProblemResponseException) e.getCause()).getProblem();
        // handle problem
    }
}
```

**Migration Steps:**

1. Replace `org.zalando.problem.Problem` with `org.springframework.http.ProblemDetail`
2. Replace `org.zalando.problem.Exceptional` with `org.zalando.riptide.problem.ProblemResponseException`
3. Update exception handling to unwrap `ProblemDetail` from `ProblemResponseException`
4. Remove `org.zalando:problem` dependency as it is no longer needed
5. Remove `org.zalando:jackson-datatype-problem` dependency

### Spring Framework Changes

**MediaType.SPECIFICITY_COMPARATOR Removed:**

Spring 7 removed the deprecated `MediaType.SPECIFICITY_COMPARATOR`. Riptide now uses a custom comparator internally based on `MediaType.isMoreSpecific()`.

This change is Riptide internal and should not affect user code unless you were directly using this constant.

### Spring Boot AutoConfiguration Package Changes

Spring Boot 4 reorganizes some autoconfiguration classes into more specific packages:

**Jackson AutoConfiguration:**
- Old: `org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration`
- New: `org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration`

**Metrics AutoConfiguration:**
- Old: `org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration`
- New: `org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration`

If you're importing these autoconfiguration classes in your tests (e.g., with `@ImportAutoConfiguration`), you'll need to update the import statements.

### Compatibility Module - ListenableFuture Removal

Spring Framework 7 removed `ListenableFuture` and related classes. As a result, Riptide 5.0 removes the `CompletableToListenableFutureAdapter` from the `riptide-compatibility` module.

**Removed class:**
- `org.zalando.riptide.compatibility.CompletableToListenableFutureAdapter`

**Migration:**
If you were using this adapter, you need to migrate to `CompletableFuture` directly. Spring Framework 7 recommends using `CompletableFuture` or reactive types (Reactor, RxJava) instead of `ListenableFuture`.

Before (Riptide 4.x):
```java
ListenableFuture<ClientHttpResponse> future = 
    new CompletableToListenableFutureAdapter<>(http.get("/api").call(pass()));
```

After (Riptide 5.x):
```java
CompletableFuture<ClientHttpResponse> future = http.get("/api").call(pass());
```

### Dependency Version Updates

The following dependencies have been updated:

| Dependency | Riptide 4.x | Riptide 5.x |
|------------|-------------|-------------|
| Spring Framework | 6.2.x | 7.0.x |
| Spring Boot | 3.1.x | 4.0.x |
| Jackson | 2.18.x | 3.0.x |
| Apache HttpClient | 5.3.x | 5.5.x |
| JUnit Jupiter | 5.12.x | 6.0.x |
| Logbook | 3.11.x | 4.0.0-RC.1 |

### Testing Changes

If you use Jackson in your tests, update your test setup:

**Before:**
```java
private static MappingJackson2HttpMessageConverter createJsonConverter() {
    final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
    return converter;
}
```

**After:**
```java
import tools.jackson.databind.json.JsonMapper;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

private static JacksonJsonHttpMessageConverter createJsonConverter() {
    var mapper = JsonMapper.builder().build();
    return new JacksonJsonHttpMessageConverter(mapper);
}
```

### Streams Module Changes

The `riptide-stream` module has been updated to use Jackson 3.x APIs. If you're using the Streams module, note the following changes:

**ObjectMapper replaced with JsonMapper:**

Before (Riptide 4.x):
```java
import com.fasterxml.jackson.databind.ObjectMapper;

StreamConverter<T> converter = Streams.streamConverter(new ObjectMapper());
```

After (Riptide 5.x):
```java
import tools.jackson.databind.json.JsonMapper;

StreamConverter<T> converter = Streams.streamConverter(new JsonMapper());
```

**Internal API changes:**
- `ObjectMapper.getFactory().createParser()` → `JsonMapper.createParser()`
- `parser.getCodec().readValue()` → `parser.readValueAs()`
- Removed deprecated `TypeFactory.constructType()` overload with context class

These changes are mostly internal to the Streams module, but if you're extending or customizing the stream converters, you'll need to update to the Jackson 3.x APIs.

## Summary of Breaking Changes

1. **Spring 7 / Spring Boot 4 required** - Update all Spring dependencies
2. **Jackson 3.x migration** - Update Jackson imports and dependencies
3. **HTTP Message Converter renamed** - Use `JacksonJsonHttpMessageConverter` instead of `MappingJackson2HttpMessageConverter`
4. **Problem library removed** - Use Spring's `ProblemDetail` instead of Zalando's `Problem`
5. **Removed Jackson modules** - Explicitly add if needed: `jackson-module-parameter-names`, `jackson-datatype-jdk8`
6. **AutoConfiguration package changes** - Update imports for `JacksonAutoConfiguration` and `MetricsAutoConfiguration`
7. **Compatibility module** - `CompletableToListenableFutureAdapter` removed; use `CompletableFuture` directly
8. **Streams module** - Use `JsonMapper` instead of `ObjectMapper` for stream converters
9. **Dependency updates** - Apache HttpClient 5.5.x, JUnit Jupiter 6.x, Logbook 4.x

## Migration Checklist

- [ ] Update to Spring Boot 4.0.0 or later
- [ ] Update Jackson imports from `com.fasterxml.jackson` to `tools.jackson` where applicable
- [ ] Replace `MappingJackson2HttpMessageConverter` with `JacksonJsonHttpMessageConverter`
- [ ] Replace Zalando `Problem` with Spring `ProblemDetail`
- [ ] Update exception handling for `ProblemResponseException`
- [ ] Remove `org.zalando:problem` and `org.zalando:jackson-datatype-problem` dependencies if no longer needed
- [ ] Add back any Jackson modules you need explicitly
- [ ] Update autoconfiguration imports (`JacksonAutoConfiguration`, `MetricsAutoConfiguration`)
- [ ] Replace `CompletableToListenableFutureAdapter` usage with `CompletableFuture` (if using compatibility module)
- [ ] If using Streams module, replace `ObjectMapper` with `JsonMapper` in stream converters
- [ ] Update test code to use Jackson 3.x APIs
- [ ] Test thoroughly - Jackson 3.x has behavioral changes

# Riptide 4.0 Migration Guide

## Before you start

**Riptide 4** requires Java 17 or up.
**Riptide 4** requires Spring 6 or up.

## Failsafe

**Riptide now requires Failsafe 3.3.x**

There are many breaking changes between Failsafe version 2.4.3 and version 3.3.0, 
see [Failsafe CHANGELOG](https://github.com/failsafe-lib/failsafe/blob/master/CHANGELOG.md#330) for all details.    
Here are some of the breaking changes that can affect `riptide-failsafe` users:

- The maven group id for Failsafe has changed to `dev.failsafe`
- All files have been moved to the `dev.failsafe` package
- `Scheduler`, `DefaultScheduledFuture` and `PolicyExecutor` were moved to the spi package
- All policies now use a builder API instead of constructors
- `DelayFunction` interface has been removed, `ContextualSupplier` should be used instead since it provides access to the same information
- `CircuitBreakerBuilder` `onOpen`, `onClose`, and `onHalfOpen` methods now accept an `EventListener<CircuitBreakerStateChangedEvent>` argument

## Spring

Since Spring 5, `AsyncRestTemplate` is deprecated in favor of `WebClient`.
For that reason, we have removed `AsyncHttpOperations` from `riptide-compatibility` layer.

Since Spring 6, `AsyncRestTemplate` and all corresponding classes like `AsyncClientHttpRequestFactory` have been removed.
For that reason, we have removed `NonBlockingIO` from `riptide-core` to avoid additionally migrating to `WebClient`.
The same reason applies to the removal of `HttpOutputMessageAsyncClientHttpRequestAdapter` from `riptide-compatibility-layer`.

## Apache HttpClient 5

Apache HttpClient 5 removed the definition of whether a specific HTTP method is allowed to have a body or not. Due to
this `StreamingApacheClientHttpRequest::setBody` will not throw an exception anymore.

## OpenTracing

The `SpanDecorators` obtained by `ServiceLoaderSpanDecorator` 
(via the [`ServiceLoader`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ServiceLoader.html) facility) 
are loaded eagerly and only once.

## OpenTracing FlowId Starter

As the project is in maintenance mode and no changes are planned anymore including support for newer Spring versions,
it was decided to include an adapted copy into Riptide itself, so the dependency is not needed anymore.

# Riptide 3.0 Migration Guide

## Before You Start

**Riptide 3** requires Spring 4.1.7 or up.
The **Starter and Auto Configuration require Spring Boot 2.2** though.

If you are running Spring Boot 1.x with Spring 4, consider using [Riptide 2.x](https://github.com/zalando/riptide/releases/tag/2.11.0).

Modules have been added/removed/changed and the configuration structure in your `application.yml` will most like require change. Please read the following sections carefully:

## New Features

- [Authorization](riptide-auth)
- Caching
- [Chaos and Fault Injection](riptide-chaos)
- [Async/RestTemplate Compatibility](riptide-compatibility)
- [Idempotency Detection](riptide-idempotency)
- [Native Logbook support](riptide-logbook)
- [OpenTracing](riptide-opentracing)
- [SOAP](riptide-soap)

## Async/RestTemplate support

Riptide 2.x had a
[`PluginInterceptor`](https://github.com/zalando/riptide/blob/2.11.0/riptide-core/src/main/java/org/zalando/riptide/PluginInterceptor.java) which allowed to use a Riptide `Plugin` in an ordinary `RestTemplate` or `AsyncRestTemplate`. The Riptide Spring Boot Auto Configuration also made use of that and registered both kinds of templates for each configured client, allowing to inject them instead of an `Http` instance:

```java
@Autowired
Client(RestTemplate example) {
    // ...
}
```

Both, the `PluginInterceptor` as well as pre-configured templates have been removed. As a cleaner alternative Riptide now offers a [Compatibility](riptide-compatibility) module which includes custom implementations of Spring's `RestOperations` and `AsyncRestOperations` which use Riptide under the hood. The Auto Configuration registers an instance of both automatically:

```java
@Autowired
Client(RestOperations example) {
    // ...
}
```

## Failsafe

**Riptide now requires Failsafe 2.x**

- The `TimeoutPlugin` (`riptide-timeout`) has been removed in favor of Failsafe's `Timeout` policy
- The `BackupRequestPlugin` (`riptide-backup`) has been removed in favor of a custom Failsafe `BackupRequest` policy
- The `FailsafePlugin` no longer needs a custom scheduler but rather uses Failsafe's default

## Logbook

**Riptide now requires Logbook 2.x**

Riptide used to rely on Logbook's integration for the Apache HTTP Client. The usages of the `LogbookHttpRequestInterceptor` and `LogbookHttpResponseInterceptor` have been replaced by a [`LogbookPlugin`](riptide-logbook/src/main/java/org/zalando/riptide/logbook/LogbookPlugin.java).

:warning: Please be aware that for tests you will need to register a `Logbook` instance by hand, e.g. by doing this:

```java
@ImportAutoConfiguration(LogbookAutoConfiguration.class)
class MyTest {
    // ...
}
```

## Micrometer

Module, package and plugin was renamed:

- `riptide-metrics` is now `riptide-micrometer`
- `org.zalando.riptide.metrics` is now `org.zalando.riptide.micrometer`
- `MetricsPlugin` is now `MicrometerPlugin`

## OAuth

STUPS OAuth 2.0 Token support has been dropped in favor of
[K8s](https://kubernetes-on-aws.readthedocs.io/en/latest/user-guide/zalando-iam.html).

## Request compression

The Apache HTTP client specific `GzipHttpRequestInterceptor` has been replaced with a request factory agnostic [`RequestCompressionPlugin`](riptide-core/src/main/java/org/zalando/riptide/RequestCompressionPlugin.java). 

## Spring Boot Auto Configuration

### Changed resolution of dependency beans

Riptide 2.x was looking for specific beans named `meterRegistry`, `logbook` or `tracer` during the construction of appropriate plugins. This behaviour changed to resolution by type. 

### Added `enabled` properties

:warning: **All nested configurations now have an `enabled` flag:**

- added `riptide.defaults.backup-request.enabled` (default: `false`)
- added `riptide.defaults.certificate-pinning.enabled` (default: `false`)
- added `riptide.defaults.circuit-breaker.enabled` (default: `false`)
- added `riptide.defaults.logging.enabled` (default: `false`️)
- added `riptide.defaults.metrics.enabled` (default: `false`)
- added `riptide.defaults.auth.enabled` (default: `false`)
- added `riptide.defaults.request-compression.enabled` (default: `false`)
- added `riptide.defaults.retry.enabled` (default: `false`)
- added `riptide.defaults.retry.backoff.enabled` (default: `false`)
- added `riptide.defaults.stack-trace-preservation.enabled` (default: `true`)
- added `riptide.defaults.timeouts.enabled` (default: `false`)
- added `riptide.defaults.transient-fault-detection.enabled` (default: `false`)

### Renamed properties

| Before                                           | After                                                        |
|--------------------------------------------------|--------------------------------------------------------------|
| `riptide.oauth.credentials-directory`            | `riptide.defaults.auth.credentials-directory`                |
| `riptide.defaults.keystore.password`             | `riptide.defaults.certificate-pinning.keystore.password`     |
| `riptide.defaults.keystore.path`                 | `riptide.defaults.certificate-pinning.keystore.path`         |
| `riptide.defaults.connect-timeout`               | `riptide.defaults.connections.connect-timeout`               |
| `riptide.defaults.max-connections-per-route`     | `riptide.defaults.connections.max-per-route`                 |
| `riptide.defaults.max-connections-total`         | `riptide.defaults.connections.max-total`                     |
| `riptide.defaults.socket-timeout`                | `riptide.defaults.connections.socket-timeout`                |
| `riptide.defaults.connection-time-to-live`       | `riptide.defaults.connections.time-to-live`                  |
| `riptide.defaults.record-metrics`                | `riptide.defaults.metrics.enabled`                           |
| `riptide.defaults.compress-request`              | `riptide.defaults.request-compression.enabled`               |
| `riptide.defaults.preserve-stack-trace`          | `riptide.defaults.stack-trace-preservation.enabled`          |
| `riptide.defaults.thread-pool.keep-alive`        | `riptide.defaults.threads.keep-alive`                        |
| `riptide.defaults.thread-pool.max-size`          | `riptide.defaults.threads.max-size`                          |
| `riptide.defaults.thread-pool.min-size`          | `riptide.defaults.threads.min-size`                          |
| `riptide.defaults.thread-pool.queue-size`        | `riptide.defaults.threads.queue-size`                        |
| `riptide.defaults.timeout`                       | `riptide.defaults.timeouts.global`                           |
| `riptide.defaults.detect-transient-faults`       | `riptide.defaults.transient-fault-detection.enabled`         |
| `riptide.clients.<id>.keystore.password`         | `riptide.clients.<id>.certificate-pinning.keystore.password` |
| `riptide.clients.<id>.keystore.path`             | `riptide.clients.<id>.certificate-pinning.keystore.path`     |
| `riptide.clients.<id>.connect-timeout`           | `riptide.clients.<id>.connections.connect-timeout`           |
| `riptide.clients.<id>.max-connections-per-route` | `riptide.clients.<id>.connections.max-per-route`             |
| `riptide.clients.<id>.max-connections-total`     | `riptide.clients.<id>.connections.max-total`                 |
| `riptide.clients.<id>.socket-timeout`            | `riptide.clients.<id>.connections.socket-timeout`            |
| `riptide.clients.<id>.connection-time-to-live`   | `riptide.clients.<id>.connections.time-to-live`              |
| `riptide.clients.<id>.record-metrics`            | `riptide.clients.<id>.metrics.enabled`                       |
| `riptide.clients.<id>.compress-request`          | `riptide.clients.<id>.request-compression.enabled`           |
| `riptide.clients.<id>.preserve-stack-trace`      | `riptide.clients.<id>.stack-trace-preservation.enabled`      |
| `riptide.clients.<id>.thread-pool.keep-alive`    | `riptide.clients.<id>.threads.keep-alive`                    |
| `riptide.clients.<id>.thread-pool.max-size`      | `riptide.clients.<id>.threads.max-size`                      |
| `riptide.clients.<id>.thread-pool.min-size`      | `riptide.clients.<id>.threads.min-size`                      |
| `riptide.clients.<id>.thread-pool.queue-size`    | `riptide.clients.<id>.threads.queue-size`                    |
| `riptide.clients.<id>.timeout`                   | `riptide.clients.<id>.timeouts.global`                       |
| `riptide.clients.<id>.detect-transient-faults`   | `riptide.clients.<id>.transient-fault-detection.enabled`     |

### Removed properties

- removed `riptide.oauth.access-token-url`
- removed `riptide.oauth.scheduling-period`
- removed `riptide.oauth.connect-timeout`
- removed `riptide.oauth.socket-timeout`
- removed `riptide.clients.<id>.oauth.access-token-url`
- removed `riptide.clients.<id>.oauth.scheduling-period`
- removed `riptide.clients.<id>.oauth.connect-timeout`
- removed `riptide.clients.<id>.oauth.socket-timeout`

## Tracer

**Riptide now uses [OpenTracing Flow-ID](https://github.com/zalando/opentracing-toolbox/tree/master/opentracing-flowid) instead of Tracer. OpenTracing is now a prerequisite for `X-Flow-ID` support.**

Riptide used to rely on Tracer's integration for the Apache HTTP Client. The usages of the `TracerHttpRequestInterceptor` have been replaced partially by the [`OpenTracingPlugin`](riptide-opentracing/src/main/java/org/zalando/riptide/opentracing/OpenTracingPlugin.java) and the new `FlowHttpRequestInterceptor` (provided by `opentracing-flowid-httpclient`).
