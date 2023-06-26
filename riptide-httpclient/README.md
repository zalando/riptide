# Riptide: HTTP Client

[![Feather](../docs/feather.jpg)](https://pixabay.com/en/plumage-feather-bird-pink-violet-176723/)

[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-httpclient.svg)](http://www.javadoc.io/doc/org.zalando/riptide-httpclient)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-httpclient.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-httpclient)

*Riptide: HTTP Client* offers an alternative integration of Spring's RestTemplate (or `ClientHttpRequestFactory`) and Apache's HTTP Client.

## Example

```java
final Http http = Http.builder()
        .requestFactory(new ApacheClientHttpRequestFactory(client))
        .build();
```

## Features

- independent from *Riptide: Core*, i.e. it can be used with a plain [`RestTemplate`](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html)
- fixes several issues with Spring's [`HttpComponentsClientHttpRequestFactory`](http://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/client/HttpComponentsClientHttpRequestFactory.html):
    - preserve the underlying client's request config
    - releasing connections back to the pool after closing streams
    - aborts connection when the stream hasn't been consumed fully

## Dependencies

- Java 17

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-httpclient</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

The majority of configuration is done on the underlying `HttpClient`:

```java
CloseableHttpClient client = HttpClientBuilder.create()
        // TODO configure client here
        .build();

final Http http = Http.builder()
        .requestFactory(new ApacheClientHttpRequestFactory(client))
        .build();
```

What the `ApacheClientHttpRequestFactory` in addition offers two modes of operation:

```java
new ApacheClientHttpRequestFactory(client, Mode.BUFFERING)
```

<dl>
  <dt>Streaming (default)</dt>
  <dd>
    Streams request bodies directly to the server. This requires less
    memory but any serialization error would result in an invalid, partial
    request to the server.
  </dd>
  <dt>Buffering</dt>
  <dd>
    Buffers request bodies before sending anything to the server.
    This requires more memory but allows to catch serialization early
    without the server noticing.
  </dd>
</dl>

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
