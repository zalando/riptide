# Riptide: Auth

[![Droplets on fence](../docs/droplets.jpg)](https://pixabay.com/photos/droplets-drops-rain-geometric-217034/)

[![Build Status](https://img.shields.io/travis/zalando/riptide.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide.svg)](https://coveralls.io/r/zalando/riptide)
[![Code Quality](https://img.shields.io/codacy/grade/1fbe3d16ca544c0c8589692632d114de/master.svg)](https://www.codacy.com/app/whiskeysierra/riptide)
[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-auth.svg)](http://www.javadoc.io/doc/org.zalando/riptide-auth)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-auth.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-auth)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

*Riptide: Auth* adds authentication and authorization support to *Riptide*.

## Example

```java
Http http = Http.builder()
    .executor(..)
    .requestFactory(..)
    .plugin(new AuthorizationPlugin(
        new BasicAuthorizationProvider("username", "password")
    ))
    .build();
```

```http
HTTP/1.1 GET /example
Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ=
```

## Features

- Automatically sets `Authorization` headers on each request
- [Basic Access Authentication (RFC 7617)](https://tools.ietf.org/html/rfc7617)
- [OAuth 2.0 Bearer Token (RFC 6750)](https://tools.ietf.org/html/rfc6750)
  - Based on [Zalando's Platform IAM K8s integration](https://kubernetes-on-aws.readthedocs.io/en/latest/user-guide/zalando-iam.html)
- Direct replacement of [Tokens](https://github.com/zalando/tokens) library

## Dependencies

- Java 8
- Riptide: Core

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-auth</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Usage

The `AuthorizationPlugin` requires an `AuthorizationProvider`.

### Basic Access Authentication (Basic Auth)

The most primitive authorization provider is the `BasicAuthorizationProvider` which supports [Basic Access Authentication (RFC 7617)](https://tools.ietf.org/html/rfc7617):

```java
new BasicAuthorizationProvider("username", "password")
```

See the [example](#example) above.

### [Zalando Platform IAM (OAuth 2.0)](https://kubernetes-on-aws.readthedocs.io/en/latest/user-guide/zalando-iam.html)

Internally at Zalando we use a K8s secrets (called *platform credentials*) that are mounted as files and rotated on regular basis. The mounted directory structure looks like this:

```
meta
└── credentials
    ├── example-token-secret
    └── example-token-type
```

The built-in `PlatformCredentialsAuthorizationProvider` reads those:

```java
new PlatformCredentialsAuthorizationProvider("example")
```

Given a type `Bearer` and a token `eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.e30.` it will produce the following `Authorization` header:

```http
HTTP/1.1 GET /example
Authorization: Bearer eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.e30.
```

### Custom Authorization

The `AuthorizationProvider` is a pretty simple interface:

```java
public interface AuthorizationProvider {
    String get() throws IOException;
}
```
 
It and can be implemented directly if needed:

```java
new AuthorizationPlugin(() -> "token " + readTokenFromSomeWhere());
```

### Override

If an `Authorization` header is specified directly it will take precedence and the configured authorization provider will step back:

```java
http.get("/example")
    .header("Authorization", "Bearer " + token)
    .dispatch(..);
```

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
