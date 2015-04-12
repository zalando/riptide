# Riptide

[![Tidal wave](docs/wave.jpg)](http://pixabay.com/en/wave-water-sea-tsunami-giant-wave-11061/)

[![Build Status](https://travis-ci.org/whiskeysierra/riptide.svg)](https://travis-ci.org/whiskeysierra/riptide)
[![Coverage Status](https://coveralls.io/repos/whiskeysierra/riptide/badge.png)](https://coveralls.io/r/whiskeysierra/riptide)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide)

A response dispatcher for Spring's RestTemplate. Riptide adds a customizable dispatcher to
Spring's RestTemplate that allows you to handle different status codes, content types, etc.
differently with an easy to use syntax.

## Dependency

    <dependency>
        <groupId>org.zalando</groupId>
        <artifactId>riptide</artifactId>
        <version>${riptide.versions}</version>
    </dependency>

## Usage

    template.execute("http://example.com", GET, null, from(template).dispatch(statusCode(),
            consume(HttpStatus.OK, Happy.class, this::onSuccess),
            consume(HttpStatus.NOT_FOUND, String.class, message -> {
                throw new NotFoundException(message);
            })
    ));
    
    private void onSuccess(Happy happy) {
        // do something with happy here...
    }

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
