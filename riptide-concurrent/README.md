# Riptide: Concurrent

[![Rope](../docs/rope.jpg)](https://pixabay.com/photos/rain-water-drip-drop-of-water-wire-57202/)

[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-concurrent.svg)](http://www.javadoc.io/doc/org.zalando/riptide-concurrent)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-concurrent.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-concurrent)

*Riptide: Concurrent* offers a wizard-like API to safely and comprehensively construct a `java.util.concurrent.ThreadPoolExecutor`.
In addition, it also comes with a *scale-first* policy, a non-standard, but highly desirable thread pool behavior of scaling up threads, before starting to queue tasks.

## Example

```java
var pool = ThreadPoolExecutors.builder()
    .elasticSize(5, 20)
    .keepAlive(Duration.ofMinutes(1))
    .scaleFirst()
    .boundedQueue(20)
    .build()
```

## Features

* Fluent `ThreadPoolExecutor` builder
* Dependency-free, i.e. can be used w/o the Riptide ecosystem
* Great developer experience in an IDE with code-completion
* Safe against misuse, i.e. less runtime errors due to
* Scale-first thread pools

## Dependencies

- Java 17

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-concurrent</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Usage

The Builder API is essentially a state machine that will guide you through the process of constructing a `ThreadPoolExecutor`.
It will do so by only offering options in combinations that actually make sense and will work.

As an example, the combination of *queue-first* and an unbounded queue makes no sense.
The pool would never grow, since everything will be queued.
As a consequence, the following code **won't even compile**:

```java
var pool = ThreadPoolExecutors.builder()
    .elasticSize(10, 20)
    .queueFirst()
    .unboundedQueue()
    .build();
```

You can take a look at the following diagram to see the process:

<details>
  <summary>State diagram</summary>

*This will be rendered as an image, if you open it in IntelliJ IDEA with the Markdown plugin and Plantuml extension enabled.*

```plantuml
hide empty description

[*] --> Start

Start --> FixedSize: fixedSize
Start --> ElasticSize: elasticSize

FixedSize --> Threads: withoutQueue
FixedSize --> Threads: boundedQueue
FixedSize --> Threads: unboundedQueue

ElasticSize --> KeepAliveTime: keepAlive

KeepAliveTime --> Threads: withoutQueue
KeepAliveTime --> QueueFirst: queueFirst
KeepAliveTime --> ScaleFirst: scaleFirst

QueueFirst --> Threads: boundedQueue
ScaleFirst --> Threads: boundedQueue
ScaleFirst --> Threads: unboundedQueue

Threads --> [*]: build
Threads --> PreStart: threadFactory
Threads --> Build: handler
PreStart --> RejectedExecutions: preStartThreads
PreStart --> Build: handler
RejectedExecutions --> [*]: build
RejectedExecutions --> Build: handler

Build --> [*]: build
```

</details>

Please refer to the following code blocks for examples:

### Fixed pool

```java
var pool = ThreadPoolExecutors.builder()
    .fixedSize(20)
    .boundedQueue(20)
    .threadFactory(new CustomizableThreadFactory("my-prefix-"))
    .preStartThreads()
    .handler(new CallerRunsPolicy())
    .build();
```

### Elastic pool

```java
var pool = ThreadPoolExecutors.builder()
    .elasticSize(5, 20)
    .keppAlive(Duration.ofMinutes(1))
    .scaleFirst()
    .boundedQueue(20)
    .threadFactory(new CustomizableThreadFactory("my-prefix-"))
    .preStartThreads()
    .handler(new CallerRunsPolicy())
    .build();
```

### Elastic vs fixed size?

The very first decision that you need to make is whether a fixed or elastic thread pool is needed.

 * `fixedSize(int poolSize)`  
   A fixed size thread pool will start off empty and ultimately grow to its maximum size.
   Once it's at the maximum, it will stay there and never shrink back.
 * `elasticSize(int corePoolSize, int maximumPoolSize)`  
   An elastic thread pool on the other hand has a core, and a maximum pool size.
   If it's idling, it will shrink down to its core pool size.
   The maximum time an idling thread is kept alive is configurable.

### Without queue vs un/bounded queue?

In general, one has the following options when deciding for a work queue:

 * `withoutQueue()`  
   No work queue, i.e. reject tasks if no thread is available
 * `boundedQueue(int)`  
   A work queue with a maximum size, i.e. rejects tasks if no thread is available **and** work queue is full
 * `unboundedQueue()`  
   A work queue without a maximum size, i.e. it never rejects tasks

:rotating_light: **Unbounded queues** are risky in production since they will grow without limits and may either hide scaling/latency issues, consume too much memory or even both.

### Queue vs scale first?

Elastic pools that use a work queue have two options:

 * `queueFirst()`  
   First queue tasks until the work queue is full, then start additional threads.
 * `scaleFirst()`  
   First start new threads until the pool reaches its maximum, then start queuing tasks.

The `ThreadPoolExecutor`'s default behavior (w/o using this library) is *queue-first*.
Most applications would benefit from defaulting to a *scale-first* policy though.

### Optional configuration

 * `threadFactory(ThreadFactory)`  
   * e.g. Spring's `CustomizableThreadFactory` which adds a thread name prefix
 * `preStartThreads()` or `preStartThreads(boolean)`  
   * Eagerly starts threads immediately, instead of *when needed*
     * Elastic pools will start all core threads
     * Fixed pools will start all threads
   * Defaults to `false`
 * `handler(RejectedExecutionHandler)`, for example:
   * `AbortPolicy` (default)
   * `CallerRunsPolicy`
   * `DiscardOldestPolicy`
   * `DiscardPolicy`
   * or a custom `RejectedExecutionHandler`

```java
ThreadPoolExecutors.builder()
    // ...
    .threadFactory(new CustomizableThreadFactory("prefix-"))
    .preStartThreads()
    .handler(new CallerRunsPolicy())
    .build():
```

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](../.github/CONTRIBUTING.md).

# Credits and references 

 * [Java Scale First ExecutorService — A myth or a reality](https://medium.com/@uditharosha/java-scale-first-executorservice-4245a63222df)
