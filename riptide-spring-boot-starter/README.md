# Riptide: Spring Boot Starter

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
       - optional gzipping of request body
       - optional pinning of a trusted keystore
  - [Hystrix](https://github.com/Netflix/Hystrix)
- [Spring Boot](http://projects.spring.io/spring-boot/) Auto Configuration
- Sensible defaults

## Dependencies

- Java 8
- Any build tool using Maven Central, or direct download
- Spring Boot
- Riptide
- Logbook
- Tracer
- Tokens
- Apache HTTP Client
- Hystrix

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

You can now define new REST clients and override default configuration in your `application.yml`:

```yaml
riptide:
  defaults:
    connection-timeout: 1 seconds
    socket-timeout: 2 seconds
    connection-time-to-live: 30 seconds
    max-connections-per-route: 16
    max-connections-total: 16
    plugins:
      - original-stack-trace
  oauth:
    access-token-url: https://auth.example.com
    credentials-directory: /secrets
    scheduling-period: 10 seconds
    connection-timeout: 1 second
    socket-timeout: 1500 milliseconds
  clients:
    example:
      base-url: https://example.com
      connection-timeout: 2 seconds
      socket-timeout: 3 seconds
      oauth.scopes:
        - uid
        - example.read
      plugins:
        - original-stack-trace
        - temporary-exception
        - hystrix
      compress-request: true
    trusted:
      base-url: https://my.trusted.com
      keystore:
        path: trusted.keystore
        password: passphrase
```

Clients are identified by a *Client ID*, for instance `example` in the sample above. You can have as many clients as you want.

For a complete overview of available properties, they type and default value please refer to the following table:

| Configuration                                    | Type           | Required | Default                                          |
|--------------------------------------------------|----------------|----------|--------------------------------------------------|
| `riptide.defaults.connection-timeout`            | `TimeSpan`     | no       | `5 seconds`                                      |
| `riptide.defaults.socket-timeout`                | `TimeSpan`     | no       | `5 seconds`                                      |
| `riptide.defaults.connection-time-to-live`       | `TimeSpan`     | no       | `30 seconds`                                     |
| `riptide.defaults.max-connections-per-route`     | `int`          | no       | `2`                                              |
| `riptide.defaults.max-connections-total`         | `int`          | no       | maximum of `20` and *per route*                  |
| `riptide.defaults.plugins`                       | `List<String>` | no       | `[original-stack-trace]`                         |
| `riptide.oauth.access-token-url`                 | `URI`          | no       | env var `ACCESS_TOKEN_URL`                       |
| `riptide.oauth.credentials-directory`            | `Path`         | no       | env var `CREDENTIALS_DIR`                        |
| `riptide.oauth.scheduling-period`                | `TimeSpan`     | no       | `5 seconds`                                      |
| `riptide.oauth.connetion-timeout`                | `TimeSpan`     | no       | `1 second`                                       |
| `riptide.oauth.socket-timeout`                   | `TimeSpan`     | no       | `2 seconds`                                      |
| `riptide.oauth.connection-time-to-live`          | `TimeSpan`     | no       | see `riptide.defaults.connection-time-to-live`   |
| `riptide.clients.<id>.base-url`                  | `URI`          | no       | none                                             |
| `riptide.clients.<id>.connection-timeout`        | `TimeSpan`     | no       | see `riptide.defaults.connection-timeout`        |
| `riptide.clients.<id>.socket-timeout`            | `TimeSpan`     | no       | see `riptide.defaults.socket-timeout`            |
| `riptide.clients.<id>.connection-time-to-live`   | `TimeSpan`     | no       | see `riptide.defaults.connection-time-to-live`   |
| `riptide.clients.<id>.max-connections-per-route` | `int`          | no       | see `riptide.defaults.max-connections-per-route` |
| `riptide.clients.<id>.max-connections-total`     | `int`          | no       | see `riptide.defaults.max-connections-total    ` |
| `riptide.clients.<id>.oauth`                     |                | no       | none, disables OAuth2 if omitted                 |
| `riptide.clients.<id>.oauth.scopes`              | `List<String>` | no       | none                                             |
| `riptide.clients.<id>.plugins`                   | `List<String>` | no       | `[original-stack-trace]`                         |
| `riptide.clients.<id>.compress-request`          | `boolean`      | no       | `false`                                          |
| `riptide.clients.<id>.keystore.path`             | `String`       | no       | none                                             |
| `riptide.clients.<id>.keystore.password`         | `String`       | no       | none                                             |

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

[![Client Dependency Graph](docs/graph.png)](https://raw.githubusercontent.com/zalando/riptide/master/docs/graph.png)

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
