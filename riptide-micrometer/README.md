# Riptide: Micrometer

[![Gauge](../docs/gauge.jpg)](https://pixabay.com/en/pressure-gauge-meter-water-column-2644531/)

[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-micrometer.svg)](http://www.javadoc.io/doc/org.zalando/riptide-micrometer)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-micrometer.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-micrometer)

*Riptide: Micrometer* adds metrics support to *Riptide*. It allows to record metrics for all remote requests and responses. 

## Example

```java
Http.builder()
    .plugin(new MicrometerPlugin(meterRegistry))
    .build();
```

## Features

- adds request metrics to Riptide

## Dependencies

- Riptide Core
- [Micrometer](https://micrometer.io/)

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-micrometer</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

```java
Http.builder()
    .baseUrl("https://www.example.com")
    .plugin(new MicrometerPlugin(meterRegistry)
    .build();
```

It's also possible to specify a custom metrics name with `http.client.requests` being the default
and default tags:

```java
new MicrometerPlugin(meterRegistry)
    .withMetricName("http.outgoing-requests")
    .withDefaultTags(Tag.of("aws.region", "eu-central-1"))
```

### Histogram and percentiles

The `MicrometerPlugin` does **NOT** configure any histograms or percentiles for two reasons:

1. It would require Riptide to effectively replicate Micrometer's API and also maintain compatibility without providing any additional value that would justify it.
2. It should be left to users to decide which strategy they want to use: percentiles per instance (non-aggregatable), percentiles per cluster (aggregated centrally) or percentiles per cluster (pre-aggregated per instance)

The preferred solution is to register a custom `MeterFilter` and configure percentiles that way:

```java
registry.config().meterFilter(
    new MeterFilter() {
        @Override
        public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
            if(id.getName().startsWith("myservice")) {
                return DistributionStatisticConfig.builder()
                    .percentiles(0.95)
                    .build()
                    .merge(config);
            }
            return config;
        }
    });
```
For additional information on percentiles and histograms check the docs of [Micrometer](https://micrometer.io/docs/concepts#_histograms_and_percentiles) and [Prometheus](https://prometheus.io/docs/practices/histograms/).

## Usage

The plugin will measure network communication but exclude any logic that is part of the local routing tree, i.e. `greet`
in the following example:

```java
http.get("/users/me")
    .dispatch(series(),
        on(SUCCESSFUL).call(User.class, this::greet),
        anySeries().call(problemHandling()))
```

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply open a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).
