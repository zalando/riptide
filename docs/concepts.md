# Concepts

It allows to dispatch HTTP responses very easily to different handler methods based on any characteristic of the
response, including but not limited to status code, status family and content type. The way this works is intentionally
very similar to server-side request routing where any request that reaches a web application is usually routed to the
correct handler based on any combination of the following criteria: URI including query and path parameters, method,
`Accept` and `Content-Type` header. Instead of routing requests to handler methods on the server what *Riptide* does
is the exact opposite: routing responses to handler methods on the client side.

![Routing Tree](https://docs.google.com/drawings/d/1BRTXVtmwIMJti1l5cQMrZsfKnTfBElTB8pDSxVBQbIQ/pub?w=888&h=691)

### Route

> A Route is either a user-supplied **callback or** a nested **[routing tree](#routing-tree)**. Following a route will
  execute the callback or traverse the routing tree respectively.

```java
on(SUCCESSFUL).call(response ->
    System.out.println(response.getHeaders().getLocation()))
```

### Routing Tree

> A Routing Tree is a route that is represented as the combination of a **[navigator](#navigator) and** a set of 
  **[bindings](#binding)**.

```java
on(SUCCESSFUL).dispatch(contentType(),
    on(APPLICATION_JSON).call(..),
    on(APPLICATION_XML).call(..))
```

### Navigator

> A Navigator **chooses among** the **[bindings](#binding)** of a [routing tree](#routing-tree). 
  The act of **traversing a [routing tree](#routing-tree)** by choosing a binding and following its associated route is
  called **nested dispatch**.

| Navigator                                                                                                 | Aspect               |
|-----------------------------------------------------------------------------------------------------------|----------------------|
| [Navigators.series()](../riptide-core/src/main/java/org/zalando/riptide/SeriesNavigator.java)             | Class of status code |
| [Navigators.status()](../riptide-core/src/main/java/org/zalando/riptide/StatusNavigator.java)             | Status               |
| [Navigators.statusCode()](../riptide-core/src/main/java/org/zalando/riptide/StatusCodeNavigator.java)     | Status code          |
| [Navigators.reasonPhrase()](../riptide-core/src/main/java/org/zalando/riptide/ReasonPhraseNavigator.java) | Reason Phrase        |
| [Navigators.contentType()](../riptide-core/src/main/java/org/zalando/riptide/ContentTypeNavigator.java)   | Content-Type header  |

### Binding

> A Binding **binds an attribute to a [route](#route)**. It represents a choice to the [navigator](#navigator) which
  route to follow.

| Route                                  | Syntax                                              |
|----------------------------------------|-----------------------------------------------------|
| `ThrowingRunnable`                     | `on(..).call(ThrowingRunnable)`                     |
| `ThrowingConsumer<ClientHttpResponse>` | `on(..).call(ThrowingConsumer<ClientHttpResponse>)` |
| `ThrowingConsumer<T>`                  | `on(..).call(Class<T>, ThrowingConsumer<T>)`        |
| `ThrowingConsumer<T>`                  | `on(..).call(TypeToken<T>, ThrowingConsumer<T>)`    |
| `RoutingTree`                          | `on(..).dispatch(..)`                               |
