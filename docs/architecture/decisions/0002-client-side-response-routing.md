# 2. Client-side response routing

Date: 2019-12-10

## Status

Accepted

## Context

Higher-level HTTP clients (in Java) fundamentally try to bridge the programming model with HTTP network communication. Most of these clients tend to *squeeze* HTTP into the traditional method call style: single return value and exception cases. That has several issues. On the one hand it unnecessarily restricts the use of HTTP: There is no need to have a single success case. An API can return `200 OK`, `201 Created` and `202 Accepted` - all on the same endpoint. On the other hand it's not supportive enough: Error cases are usually neglected and while you usually get rich deserialization support for the happy cases, any kind of 4xx/5xx response will just trigger a generic `HttpClientErrorException` or something alike. If the response body is exposed, it's at most as an `InputStream` or a byte array.

Known HTTP clients that suffer from this are [Spring's `RestTemplate`](https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#rest-resttemplate), [Feign](https://github.com/OpenFeign/feign), [Retrofit](https://square.github.io/retrofit/) and JAX-RS.

## Decision

Riptide is build around the concept of [response routing](https://github.com/zalando/riptide/blob/master/docs/concepts.md) which fundamentally dictates that:

- All responses are created equal
  - fail to handle `200 OK` will produce the same error as `400 Bad Request`
  - each response will map to exactly one route
- Most routes are general purpose and applicable to any kind of response
  - A route that deserializes the body and triggers a callback will work for any status code
- Multiple happy cases are just multiple routes for different *2xx* status codes

## Consequences

The uniformity of response routing has several significant benefits:

1. Users can easily define, understand and change how different kinds of responses are routed
2. Multiple happy cases are not special and don't need to converge to a common base type 
3. Rich error responses can be handled using the same, familiar APIs as happy cases

