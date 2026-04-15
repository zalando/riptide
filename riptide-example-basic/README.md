# Riptide: Example (Basic)

This module is a runnable onboarding example. It demonstrates the core Riptide API through the
Spring Boot starter path, with complete imports and a real test.

## What it shows

- Wiring an `Http` client via `riptide-spring-boot-starter` and injecting it with `@Qualifier`
- Routing a response on HTTP status series (`SUCCESSFUL`, `REDIRECTION`)
- Extracting the `Location` header from a redirect response
- Deserializing a JSON response body into a typed object

## Running the tests

```bash
./mvnw test -pl riptide-example-basic -am
```

## Key classes

| Class | Purpose |
|---|---|
| [`BasicRoutingExample`](src/main/java/org/zalando/riptide/example/basic/BasicRoutingExample.java) | Main example class – shows the `Http` API surface |
| [`BasicRoutingExampleTest`](src/test/java/org/zalando/riptide/example/basic/BasicRoutingExampleTest.java) | JUnit 5 test – verifies redirect routing and body deserialization |

## Key Riptide API entry points

| Class | Purpose |
|---|---|
| [`Http`](../riptide-core/src/main/java/org/zalando/riptide/Http.java) | Entry point — call `.get()`, `.post()`, etc. to build requests |
| [`Navigators`](../riptide-core/src/main/java/org/zalando/riptide/Navigators.java) | Factory for navigators: `series()`, `status()`, `contentType()`, etc. |
| [`Bindings`](../riptide-core/src/main/java/org/zalando/riptide/Bindings.java) | Factory for route bindings: `on(SUCCESSFUL)`, `anySeries()`, etc. |

## Dependencies used

| Artifact | Why |
|---|---|
| `riptide-spring-boot-starter` | Starter-based `Http` client wiring and auto-configuration |
| `spring-boot-starter-test` | Spring Boot test support for the runnable example |
| `okhttp3:mockwebserver` | Mock HTTP server for tests |

## How the test client is wired

The test uses `@SpringBootTest` with `RiptideAutoConfiguration` and `JacksonAutoConfiguration`
imported explicitly. The `base-url` for the `example` client is supplied dynamically via
`@DynamicPropertySource` so it points to the local `MockWebServer` port chosen at runtime.
The `Http` bean is then injected with `@Autowired @Qualifier("example")`.

A `@Qualifier("example") HttpClientCustomizer` bean disables automatic redirect following so
that `302 Found` responses remain visible to the routing callback — this keeps the
`Location` header extraction example consistent with actual route behavior. In production you
would typically let the HTTP client follow redirects automatically, or handle them explicitly
in your routing tree if you need to inspect intermediate responses.
