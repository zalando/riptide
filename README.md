# Riptide

[![Tidal wave](docs/wave.jpg)](http://pixabay.com/en/wave-water-sea-tsunami-giant-wave-11061/)

[![Build Status](https://img.shields.io/travis/zalando/riptide.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide.svg)](https://coveralls.io/r/zalando/riptide)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide)

A response dispatcher for Spring's RestTemplate. Riptide adds a customizable dispatcher to
Spring's RestTemplate that allows you to handle different status codes, content types, etc.
differently with an easy to use syntax.

## Dependency

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Usage

```java
final Rest rest = Rest.create(new RestTemplate());

return rest.execute(GET, URI.create("https://api.example.com"))
        .dispatch(series(),
                on(SUCCESSFUL)
                        .dispatch(statusCode(),
                                on(CREATED, Success.class).capture(),
                                on(ACCEPTED, Success.class).capture(),
                                anyStatusCode().call(this::warn)),
                on(CLIENT_ERROR)
                        .dispatch(contentType(),
                                on(PROBLEM, Problem.class).call(this::onProblem),
                                on(APPLICATION_JSON, Problem.class).call(this::onProblem),
                                anyContentType().call(this::fail)),
                on(SERVER_ERROR).call(this::fail),
                anySeries().call(this::warn))
        .unpack(Success.class).orElse(null);

private void onProblem(Problem problem) {
    throw new ProblemException(problem);
}

private void warn(ClientHttpResponse response) {
    LOG.warning("Unexpected response: " + response);
}

private void fail(ClientHttpResponse response) {
    throw new AssertionError("Unexpected response: " + response);
}
```

## License

Copyright [2015] Zalando SE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
