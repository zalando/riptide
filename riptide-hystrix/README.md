# Riptide: Hystrix

[![Hystrix](https://netflix.github.com/Hystrix/images/hystrix-logo-tagline-850.png)](https://github.com/Netflix/Hystrix)

[![Build Status](https://img.shields.io/travis/zalando/riptide/master.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide/master.svg)](https://coveralls.io/r/zalando/riptide)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/org.zalando/riptide-hystrix/badge.svg)](http://www.javadoc.io/doc/org.zalando/riptide-hystrix)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-hystrix.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-hystrix)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

*Riptide: Hystrix* adds Hystrix support to *Riptide*.

## Example

```java
Rest.builder()
    .plugin(new HystrixPlugin())
    .build();
```

## Features

-  seamlessly integrates Riptide with Hystrix

## Dependencies

- Java 8
- Riptide Core
- Hystrix

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-hystrix</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

By default, Hystrix group keys match the host name. Hystrix command keys are composed of the HTTP method and the
URI template or path.

For the canonical GitHub example...

- the group key would be `api.github.com` and
- the command key would be `GET /contributors/{org}/{repo}/contributors`
  - if the request is not using a URI template it will fallback to the URI path:
    `GET /contributors/zalando/riptide/contributors`

You can use `HystrixPlugin#HystrixPlugin(SetterFactory)` to customize it:

```java
new HystrixModule(request ->
      withGroupKey(HystrixCommandGroupKey.Factory.asKey("github"))
      .andCommandKey(HystrixCommandKey.Factory.asKey(request.getMethod())))
```

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../CONTRIBUTING.md).
