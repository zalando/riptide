# Riptide: HTTP Client

[![Feather](../docs/feather.jpg)](https://pixabay.com/en/plumage-feather-bird-pink-violet-176723/)

[![Build Status](https://img.shields.io/travis/zalando/riptide.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide.svg)](https://coveralls.io/r/zalando/riptide)
[![Code Quality](https://img.shields.io/codacy/grade/1fbe3d16ca544c0c8589692632d114de/master.svg)](https://www.codacy.com/app/whiskeysierra/riptide)
[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-httpclient.svg)](http://www.javadoc.io/doc/org.zalando/riptide-httpclient)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-httpclient.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-httpclient)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

*Riptide: HTTP Client* offers an alternative integration of Spring's RestTemplate (or `ClientHttpRequestFactory`) and Apache's HTTP Client.

## Example

```java
final Http http = Http.builder()
        .requestFactory(new RestAsyncClientHttpRequestFactory(client, executor))
        .build();
```

## Features

- independent from *Riptide: Core*, i.e. it can be used with a plain [`RestTemplate`](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html)
- allows to use a plain [`HttpClient`](https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/HttpClient.html) [asynchronously](http://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/client/AsyncClientHttpRequestFactory.html)
- fixes several issues with Spring's [`HttpComponentsClientHttpRequestFactory`](http://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/client/HttpComponentsClientHttpRequestFactory.html):
    - preserve the underlying client's request config
    - releasing connections back to the pool after closing streams

## Dependencies

- Java 8

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-httpclient</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Usage

```java
CloseableHttpClient client = HttpClientBuilder.create()
        // TODO configure client here
        .build();

AsyncListenableTaskExecutor executor = new ConcurrentTaskExecutor();

final Http http = Http.builder()
        .requestFactory(new RestAsyncClientHttpRequestFactory(client, executor))
        .build();
```

The `RestAsyncClientHttpRequestFactory` implements `ClientHttpRequestFactory` **as well as** 
`AsyncClientHttpRequestFactory` and can therefore be used with both: `RestTemplate` and `AsyncRestTemplate`.

```java
RestAsyncClientHttpRequestFactory factory = ...;

RestTemplate sync = new RestTemplate(factory);
AsyncRestTemplate async = new AsyncRestTemplate(factory);
```

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
