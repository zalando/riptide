# Riptide: Backup

[![Pipes](../docs/pipes.jpg)](https://pixabay.com/en/pipe-taps-plumbing-water-valve-1821109/)

[![Build Status](https://img.shields.io/travis/zalando/riptide/master.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide/master.svg)](https://coveralls.io/r/zalando/riptide)
[![Code Quality](https://img.shields.io/codacy/grade/1fbe3d16ca544c0c8589692632d114de/master.svg)](https://www.codacy.com/app/whiskeysierra/riptide)
[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-backup.svg)](http://www.javadoc.io/doc/org.zalando/riptide-backup)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-backup.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-backup)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

*Riptide: Backup* implements the [*backup request*][abstract] pattern, also known as [*hedged requests*][article] as an
extension for Riptide.

## Example

```java
Http.builder()
    .plugin(new BackupRequestPlugin(scheduler, 100, MILLISECONDS))
    .build();
```

## Features

- adds delayed backup requests to Riptide calls

## Dependencies

- Java 8
- Riptide Core

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-backup</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

```java
Http.builder()
    .plugin(new BackupRequestPlugin(
            Executors.newSingleThreadScheduledExecutor(), 
            100, MILLISECONDS))
    .build();
```

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply open a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).

## Credits and references

- [Jeffrey Dean: Achieving Rapid Response Times in Large Online Services][abstract]
- [Jeffrey Dean and Luiz André Barroso: The Tail at Scale][article]
- [Uwe Friedrichsen: Patterns of Resilience - Fan Out, Quickest Reply](https://www.slideshare.net/ufried/patterns-of-resilience/61)

[abstract]: https://research.google.com/people/jeff/latency.html
[article]: http://www.cs.duke.edu/courses/cps296.4/fall13/838-CloudPapers/dean_longtail.pdf
