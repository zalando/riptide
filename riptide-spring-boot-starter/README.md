# Riptide: Spring Boot Starter

[![Leafs](../docs/leafs.jpg)](https://pixabay.com/en/leaf-green-foliage-green-leaves-1001679/)

[![Build Status](https://img.shields.io/travis/zalando/riptide/master.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide/master.svg)](https://coveralls.io/r/zalando/riptide)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/org.zalando/riptide-spring-boot-starter/badge.svg)](http://www.javadoc.io/doc/org.zalando/riptide-spring-boot-starter)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-spring-boot-starter.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-spring-boot-starter)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

*Riptide: Spring Boot Starter* is a library that seamlessly integrates various HTTP client-side tools in the
easiest and convenient way possible. It solves a recurring problem of bootstrapping and wiring different libraries
together whenever interaction with a remote service is required. Spinning up new clients couldn't get any easier!

- **Technology stack**: Spring Boot
- **Status**:  Beta

## Example

```yaml
riptide.clients:
  example:
    base-url: http://example.com
    oauth.scopes:
      - example.read
```

```java
@Autowired
@Qualifier("example")
private Http example;
```

## Features

- Seamless integration of:
  - [Riptide](https://github.com/zalando/riptide)
  - [Logbook](https://github.com/zalando/logbook)
  - [Tracer](https://github.com/zalando/tracer)
  - [Tokens](https://github.com/zalando-stups/tokens) (plus [interceptor](https://github.com/zalando-stups/stups-spring-oauth2-support/tree/master/stups-http-components-oauth2))
  - [Jackson 2](https://github.com/FasterXML/jackson)
  - [HttpClient](https://hc.apache.org/httpcomponents-client-ga/index.html)
  - [Failsafe](https://github.com/jhalterman/failsafe) via [Riptide: Failsafe](../riptide-failsafe)
- [Spring Boot](http://projects.spring.io/spring-boot/) Auto Configuration
- Automatically integrates and supports:
  - Transient fault detection via [Riptide: Faults](../riptide-faults)
  - HTTP JSON Streaming via [Riptide: Stream](../riptide-stream)
  - Timeouts via [Riptide: Timeout](../riptide-timeout)
- SSL certificate pinning
- Sensible defaults

## Dependencies

- Java 8
- Any build tool using Maven Central, or direct download
- Spring Boot
- Riptide
- Logbook
- Tracer
- Tokens (optional)
- Apache HTTP Client
- Failsafe (optional)
- ZMon Actuator (optional)

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-spring-boot-starter</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

If you want OAuth support you need to additionally add [stups-spring-oauth2-support](https://github.com/zalando-stups/stups-spring-oauth2-support/tree/master/stups-http-components-oauth2) to your project. 
It comes with [Tokens](https://github.com/zalando-stups/tokens) equipped. 

```xml
<!-- if you need OAuth support additionally add: -->
<dependency>
    <groupId>org.zalando.stups</groupId>
    <artifactId>stups-http-components-oauth2</artifactId>
    <version>$stups-http-components-oauth2.version}{</version>
</dependency>
```

## Configuration

You can now define new clients and override default configuration in your `application.yml`:

```yaml
riptide:
  oauth:
    access-token-url: https://auth.example.com
    credentials-directory: /secrets
    scheduling-period: 10 seconds
    connection-timeout: 1 second
    socket-timeout: 1500 milliseconds
  clients:
    example:
      base-url: http://example.com
      connection-timeout: 150 milliseconds
      socket-timeout: 100 milliseconds
      connection-time-to-live: 30 seconds
      max-connections-per-route: 16
      keep-original-stack-trace: true
      detect-transient-faults: true
      retry:
        fixed-delay: 50 milliseconds
        max-retries: 5
        max-duration: 2 second
        jitter: 25 milliseconds
      circuit-breaker:
        failure-threshold: 3 out of 5
        delay: 30 seconds
        success-threshold: 5 out of 5
      timeout: 500 milliseconds
      oauth.scopes:
        - example.read
```

Clients are identified by a *Client ID*, for instance `example` in the sample above. You can have as many clients as you want.

For a complete overview of available properties, they type and default value please refer to the following table:

| Configuration                           | Data type      | Default                                          |
|-----------------------------------------|----------------|--------------------------------------------------|
| `riptide`                               |                |                                                  |
| `├──` `defaults`                          |                |                                                  |
| `│   ├──` `connection-timeout`            | `TimeSpan`     | `5 seconds`                                      |
| `│   ├──` `socket-timeout`                | `TimeSpan`     | `5 seconds`                                      |
| `│   ├──` `connection-time-to-live`       | `TimeSpan`     | `30 seconds`                                     |
| `│   ├──` `max-connections-per-route`     | `int`          | `2`                                              |
| `│   ├──` `max-connections-total`         | `int`          | maximum of `20` and *per route*                  |
| `│   ├──` `keep-original-stack-trace`     | `boolean`      | `true`                                           |
| `│   ├──` `detect-transient-faults`       | `boolean`      | `true`                                           |
| `│   ├──` `retry`                         |                |                                                  |
| `│   │   ├──` `fixed-delay`               | `TimeSpan`     | none, mutually exclusive to `backoff`            |
| `│   │   ├──` `backoff`                   |                | mutually exclusive to `fixed-delay`              |
| `│   │   │   ├──` `delay`                 | `TimeSpan`     | none, requires `backoff.max-delay`               |
| `│   │   │   ├──` `max-delay`             | `TimeSpan`     | none, requires `backoff.delay`                   |
| `│   │   │   └──` `delay-factor`          | `double`       | `2.0`                                            |
| `│   │   ├──` `max-retries`               | `int`          | none                                             |
| `│   │   ├──` `max-duration`              | `TimeSpan`     | none                                             |
| `│   │   ├──` `jitter-factor`             | `double`       | none, mutually exclusive to `jitter`             |
| `│   │   └──` `jitter`                    | `TimeSpan`     | none, mutually exclusive to `jitter-factor`      |
| `│   ├──` `circuit-breaker`               |                |                                                  |
| `│   │   ├──` `failure-threshold`         | `Ratio`        | none                                             |
| `│   │   ├──` `delay`                     | `TimeSpan`     | none                                             |
| `│   │   └──` `success-threshold`         | `Ratio`        | none                                             |
| `│   └──` `timeout`                       | `TimeSpan`     | none                                             |
| `├──` `oauth`                             |                |                                                  |
| `│   ├──` `access-token-url`              | `URI`          | env var `ACCESS_TOKEN_URL`                       |
| `│   ├──` `credentials-directory`         | `Path`         | env var `CREDENTIALS_DIR`                        |
| `│   ├──` `scheduling-period`             | `TimeSpan`     | `5 seconds`                                      |
| `│   ├──` `connetion-timeout`             | `TimeSpan`     | `1 second`                                       |
| `│   ├──` `socket-timeout`                | `TimeSpan`     | `2 seconds`                                      |
| `│   └──` `connection-time-to-live`       | `TimeSpan`     | see `riptide.defaults.connection-time-to-live`   |
| `└──` `clients`                           |                |                                                  |
| `    └──` `<id>`                          |                |                                                  |
| `        ├──` `base-url`                  | `URI`          | none                                             |
| `        ├──` `connection-timeout`        | `TimeSpan`     | see `riptide.defaults.connection-timeout`        |
| `        ├──` `socket-timeout`            | `TimeSpan`     | see `riptide.defaults.socket-timeout`            |
| `        ├──` `connection-time-to-live`   | `TimeSpan`     | see `riptide.defaults.connection-time-to-live`   |
| `        ├──` `max-connections-per-route` | `int`          | see `riptide.defaults.max-connections-per-route` |
| `        ├──` `max-connections-total`     | `int`          | see `riptide.defaults.max-connections-total    ` |
| `        ├──` `oauth`                     |                | none, disables OAuth2 if omitted                 |
| `        ├──` `oauth.scopes`              | `List<String>` | none                                             |
| `        ├──` `keep-original-stack-trace` | `boolean`      | see `riptide.defaults.keep-original-stack-trace` |
| `        ├──` `detect-transient-faults`   | `boolean`      | see `riptide.defaults.detect-transient-faults`   |
| `        ├──` `retry`                     |                |                                                  |
| `        │   ├──` `fixed-delay`           | `TimeSpan`     | none, mutually exclusive to `backoff`            |
| `        │   ├──` `backoff`               |                | mutually exclusive to `fixed-delay`              |
| `        │   │   ├──` `delay`             | `TimeSpan`     | none, requires `backoff.max-delay`               |
| `        │   │   ├──` `max-delay`         | `TimeSpan`     | none, requires `backoff.delay`                   |
| `        │   │   └──` `delay-factor`      | `double`       | `2.0`                                            |
| `        │   ├──` `max-retries`           | `int`          | none                                             |
| `        │   ├──` `max-duration`          | `TimeSpan`     | none                                             |
| `        │   ├──` `jitter-factor`         | `double`       | none, mutually exclusive to `jitter`             |
| `        │   └──` `jitter`                | `TimeSpan`     | none, mutually exclusive to `jitter-factor`      |
| `        ├──` `circuit-breaker`           |                |                                                  |
| `        │   ├──` `failure-threshold`     | `Ratio`        | none                                             |
| `        │   ├──` `delay`                 | `TimeSpan`     | none                                             |
| `        │   └──` `success-threshold`     | `Ratio`        | none                                             |
| `        ├──` `timeout`                   | `TimeSpan`     | none                                             |
| `        ├──` `compress-request`          | `boolean`      | `false`                                          |
| `        └──` `keystore`                  |                |                                                  |
| `            ├──` `path`                  | `String`       | none                                             |
| `            └──` `password`              | `String`       | none                                             |

## Usage

After configuring your clients, as shown in the last section, you can now easily inject them:

```java
@Autowired
@Qualifier("example")
private Http example;
```

All beans that are created for each client use the *Client ID*, in this case `example`, as their qualifier.

Besides `Http`, you can also alternatively inject any of the following types per client directly:
- `RestTemplate`
- `AsyncRestTemplate`
- `ClientHttpRequestFactory`
- `AsyncClientHttpRequestFactory`
- `HttpClient`
- `ClientHttpMessageConverters`
- `AsyncListenableTaskExecutor`

A global `AccessTokens` bean is also provided.

### Trusted Keystore

A client can be configured to only connect to trusted hosts (see 
[Certificate Pinning](https://www.owasp.org/index.php/Certificate_and_Public_Key_Pinning)) by configuring the `keystore` key. Use 
`keystore.path` to refer to a *JKS*  keystore on the classpath/filesystem and (optionally) specify the passphrase via `keystore.password`.

You can generate a keystore using the [JDK's keytool](http://docs.oracle.com/javase/7/docs/technotes/tools/#security):

```bash
./keytool -importcert -file some-cert.crt -keystore my.keystore -alias "<some-alias>"
```

### Customization

For every client that is defined in your configuration the following graph of beans, indicated by the green color, will
be created:

[![Client Dependency Graph](../docs/graph.png)](../docs/graph.png)

Regarding the other colors:
- *yellow*: will be created once and then shared across different clients (if needed)
- *red*: mandatory dependency
- *grey*: optional dependency

Every single bean in the graph can optionally be replaced by your own, custom version of it. Beans can only be
overridden by name, **not** by type. As an example, the following code would add XML support to the `example` client:

```java
@Bean
@Qualifier("example")
public ClientHttpMessageConverters exampleHttpMessageConverters() {
    return new ClientHttpMessageConverters(new Jaxb2RootElementHttpMessageConverter());
}
```

The following table shows all beans with their respective name (for the `example` client) and type:

| Bean Name                              | Bean Type                                                          | Configures by default      |
|----------------------------------------|--------------------------------------------------------------------|----------------------------|
| `accessToken` (no client prefix!)      | `AccessTokens`                                                     | OAuth settings             |
| `exampleHttpMessageConverters`         | `ClientHttpMessageConverters`                                      | Text, JSON and JSON Stream |
| `exampleHttpClient`                    | `HttpClient`                                                       | Interceptors and timeouts  |
| `exampleAsyncClientHttpRequestFactory` | `AsyncClientHttpRequestFactory` **and** `ClientHttpRequestFactory` |                            |
| `exampleHttp`                          | `Http`                                                             | Base URL                   |
| `exampleRestTemplate`                  | `RestTemplate`                                                     | Base URL                   |
| `exampleAsyncRestTemplate`             | `AsyncRestTemplate`                                                | Base URL                   |
| `exampleAsyncListenableTaskExecutor`   | `AsyncListenableTaskExecutor`                                      | ConcurrentTaskExecutor     |

If you override a bean then all of its dependencies (see the [graph](#customization)), will **not** be registered,
unless required by some other bean.

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](CONTRIBUTING.md).
