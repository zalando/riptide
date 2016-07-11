# Riptide: Streams Extension


*Riptide Streams* extension allows to capture arbitrary infinite object streams via Spring's [RestTemplate](https://spring.io/guides/gs/consuming-rest/). The usage pattern is very simple


```java
@JsonAutoDetect(fieldVisibility = NON_PRIVATE)
static class Contributor {
    String login;
    int contributions;
}

public static void main(final String... args) {
    final AsyncRestTemplate template = new AsyncRestTemplate();
    final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    final rest = Rest.builder()
            .requestFactory(template.getAsyncRequestFactory())
            .converters(Arrays.asList(
                    Streams.converter(mapper),
                    new MappingJackson2HttpMessageConverter(mapper),
                    new StringHttpMessageConverter()))
            .baseUrl(baseUrl)
            .build();

    rest.get("/repos/{org}/{repo}/contributors", "zalando", "riptide").dispatch(series(),
        on(SUCCESSFUL).call(streamOf(Contributor.class), forEach(contributors ->
            System.out.println(contributor.login + " (" + contributor.contributions + ")")));
}
```

## Features

- thin wrapper around RestTemplate
- full access to the underlying HTTP client
- encourages to write more resilient clients, by forcing you to consider
  - fallbacks
  - content negotiation
  - robust error handling
- elegant syntax
- type-safety
- easy to implement repeating patterns, e.g.
  - follow redirects
  - create resource and retrieve location

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

## Configuration

Create an instance based on an existing `RestTemplate`:

```java
final RestTemplate template = new RestTemplate();
template.setErrorHandler(new PassThroughResponseErrorHandler());
final Rest rest = Rest.create(template);
```

Or alternatively an `AsyncRestTemplate`:

```java
final AsyncRestTemplate template = new AsyncRestTemplate();
template.setErrorHandler(new PassThroughResponseErrorHandler());
final AsyncRest rest = AsyncRest.create(template);
```

If you use Riptide to its full extent you probably don't want to have any [`ResponseErrorHandler`]
(http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/ResponseErrorHandler.html)
interfere with your dispatching. Therefore Riptide provides you with a *no-op* `ResponseErrorHandler`, which ensures
that Riptide handles all success and error cases.

**BEWARE** In case you're using the `OAuth2RestTemplate`: It uses the given `ResponseErrorHandler` in the wrong way,
which may result in the response body being already consumed and/or closed. To workaround this issue use our special
`OAuth2CompatibilityResponseErrorHandler` instead:

```java
final OAuth2RestTemplate template = new OAuth2RestTemplate();
template.setErrorHandler(new OAuth2CompatibilityResponseErrorHandler());
final Rest rest = Rest.create(template);
```

## Usage

Make a request and route the response to your specific handler methods/callbacks:

```java
rest.execute(GET, url).dispatch(status(),
        on(CREATED).call(Success.class, this::onSuccess),
        on(ACCEPTED).call(Success.class, this::onSuccess),
        on(BAD_REQUEST).call(this::onError),
        anyStatus().call(this::fail));
```

Your `onSuccess` method is allowed to have one of the following signatures:

```java
void onSuccess(Success success) throws Exception;
void onSuccess(ResponseEntity<Success> success) throws Exception;
```

The later one is useful if you e.g. need access to one or more header values.

Url template with variable expansion can be used in a same way as in `RestTemplate`, e.g.:

```java
rest.withUrl("https://example.com/posts/{id}?filter={filter}", postId, filter)
    .execute(GET)
    ...
```

### Selectors

Routing of responses is controlled by a `Selector`, e.g. `status()` in the former example.
A selector selects the attribute(s) of a response you want to use to route it.

Riptide comes with the following selectors:

#### [Selectors.series()](src/main/java/org/zalando/riptide/SeriesSelector.java)

[HttpStatus.Series](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/HttpStatus.Series.html)

```java
rest.execute(..).dispatch(series(),
    on(SUCCESSFUL).capture(),
    on(REDIRECTION).capture(follow),
    on(CLIENT_ERROR).call(fail),
    on(SERVER_ERROR).call(retryLater));
```

#### [Selectors.status()](src/main/java/org/zalando/riptide/StatusSelector.java)

[HttpStatus](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/HttpStatus.html)

```java
rest.execute(..).dispatch(status(),
    on(OK).capture(),
    on(CREATED).capture(),
    on(ACCEPTED).call(poll),
    on(NO_CONTENT).call(read));
```

#### [Selectors.statusCode()](src/main/java/org/zalando/riptide/StatusCodeSelector.java)

```java
rest.execute(..).dispatch(statusCode(),
    on(200).capture(),
    on(201).capture(),
    on(202).call(poll),
    on(204).call(read));
```

#### [Selectors.reasonPhrase()](src/main/java/org/zalando/riptide/ReasonPhraseSelector.java)

```java
rest.execute(..).dispatch(reasonPhrase(),
    on("OK").capture(),
    on("Created").capture(),
    on("Accepted").call(poll),
    on("No Content").call(read));
```

#### [Selectors.contentType()](src/main/java/org/zalando/riptide/ContentTypeSelector.java)

[MediaType](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/MediaType.html)

```java
rest.execute(..).dispatch(contentType(),
    on(APPLICATION_JSON).capture(Success.class),
    on(APPLICATION_XML).capture(Success.class),
    on(PROBLEM).call(Problem.class, propagate()),
    anyContentType().capture(String.class));
```

#### Custom Selector

You are free to write your own, which requires you to implement the following interface:

```java
public interface Selector<A> {

    @Nullable
    Binding<A> select(final ClientHttpResponse response, final Map<A, Binding<A>> bindings) throws IOException;

}
```

Implementation note: Since selectors are very often stateless, it feels very natural to implement them as package-local
enum singletons and expose them via static *factory* methods.

##### `EqualitySelector`

The most common type of selectors are actually *equality selectors*. The select an arbitrary attribute from the response
and select the binding with the matching value. Think of them as close relative of the *switch* statement.

```java
enum StatusSelector implements EqualitySelector<HttpStatus> {

    INSTANCE;

    @Override
    public HttpStatus attributeOf(final ClientHttpResponse response) throws IOException {
        return response.getStatusCode();
    }

}
```

An attribute can be a single scalar value but could be a complex type, based on your needs.

##### `BinarySelector`

A special version of the [`EqualitySelector`](#equalityselector) is a binary selector, i.e. only having two possible
meaningful conditions:

```java
rest.execute(POST, ..).dispatch(isCurrentRepresentation(),
    on(true).capture(),
    on(false).capture(location().andThen(location ->
        rest.execute(GET, location).dispatch(series(),
            on(SUCCESSFUL).capture(),
            anySeries().call(fail))
    )));
```

Binary selectors should be used very rarely as they are naturally very limited in terms of usability. Having a lot
of them also increases the size if your routing tree significantly. Compared to *equality selectors*, you can think
of binary selectors as being nothing more than *if* statements.

The only built-in binary selector checks whether the `Location` and `Content-Location` header are present and have the
same value, i.e. whether a client can use the response body of a `POST` without the need for a second `GET` request:

```java
enum CurrentRepresentationSelector implements BinarySelector {

    INSTANCE;

    @Override
    public Boolean attributeOf(final ClientHttpResponse response) throws IOException {
        @Nullable final String location = response.getHeaders().getFirst("Location");
        @Nullable final String contentLocation = response.getHeaders().getFirst("Content-Location");
        return Objects.nonNull(location) &&
                Objects.equals(location, contentLocation);
    }

}
```

### Conditions

[Conditions](src/main/java/org/zalando/riptide/Conditions.java)
describe which concrete attribute values you want to bind to which actions.

```java
on(SUCCESS).call(..),
on(CLIENT_ERROR).call(..),
anySeries().call(..)
```

Wildcard conditions are comparable to a `default` case in a switch. They take effect if no match was found. They are a
very powerful tool in being a resilient client, i.e. you should consider to always have one wildcard condition to
catch cases where the server introduced a new status code or content type. That way you can declare a meaningful
handling of those cases upfront already.

### Actions

After the selector determined the attribute, the condition matched on a concrete attribute value the
response will be routed to an action. An action can be one of the following types:

| Action                            | Syntax                               |
|-----------------------------------|--------------------------------------|
| n/a                               | `on(..).capture()`                   |
| `Function<ClientHttpResponse, ?>` | `on(..).capture(Function)`           |
| `Runnable`                        | `on(..).call(Runnable)`              |
| `Consumer<ClientHttpResponse>`    | `on(..).call(Consumer)`              |
| n/a                               | `on(..).capture(Class<T>)`           |
| `Function<T, ?>`                  | `on(..).capture(Class<T>, Function)` |
| `Function<ResponseEntity<T>, ?>`  | `on(..).capture(Class<T>, Function)` |
| `Consumer<T>`                     | `on(..).call(Class<T>, Consumer)`    |
| `Consumer<ResponseEntity<T>>`     | `on(..).call(Class<T>, Consumer)`    |
| Nested Routing                    | `on(..).dispatch(..)`                |

Consumers can be used to trigger some dedicated function and they work well if no return value is required.

Actions can operate on a low-level
[`ClientHttpResponses`](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/client/ClientHttpResponse.html)
as well as on custom types or typed
[`ResponseEntities`](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/ResponseEntity.html)
directly.

Functions on the other hand are used to apply a transformation and their result must be captured. Captured values can
later be retrieved, e.g. to produce a return value.

```java
final Optional<Success> success = rest.execute(..)
        .dispatch(..)
        .as(Success.class);

return success.orElse(..);
```

Alternatively, if your dispatching doesn't allow multiple happy cases, you can retrieve a value directly, without
dealing with an `Optional`:

```java
return rest.execute(..)
        .dispatch(..)
        .to(Success.class);
```

Please be aware that when using `AsyncRest` captures are returned as `Future<Capture>`, since the result may
not be available immediately:

```java
return rest.execute(..)
        .dispatch(..)
        .get(10, SECONDS)
        .to(Success.class);
```

Also note: All consumer/function based actions are **not** `java.util.function.Consumer`, `java.lang.Runnable` and
`java.util.function.Function` respectively, but custom version that support throwing checked exceptions. This should
not have any negative impact since most of the time you won't pass in a custom implementation, but rather a lambda or
a method reference.

#### Nested Routing

A special action is the *nested routing* which allows to have a very fine-grained control over how to route your
responses:

```java
Success success = rest.execute(GET, url)
        .dispatch(series(),
                on(SUCCESSFUL).capture(Success.class),
                on(CLIENT_ERROR)
                    .dispatch(status(),
                            on(UNAUTHORIZED).call(this::login),
                            on(UNPROCESSABLE_ENTITY)
                                    .dispatch(contentType(),
                                            on(PROBLEM).call(ThrowableProblem.class, propagate()),
                                            on(ERROR).call(Exception.class, propagate()))),
                on(SERVER_ERROR)
                    .dispatch(statusCode(),
                            on(503).call(this::retryLater),
                anySeries().call(this::fail))
        .as(Success.class).orElse(null);
```

If a *no match* case happens in a nested routing scenario it will bubble up the levels until it finds a matching
wildcard condition. In the example above, if the server responded with a plain `500 Internal Server Error` the
router would dispatch on the series, entering `on(SERVER_ERROR)` (5xx), try to dispatch on status code, won't find a
matching condition and neither a wildcard so it would bubble up and be *caught* by the `anySeries().call(..)`
statement.

Another feature of nested routing is to externalize and embed partial routing trees. Based on the previous example one
could extract the problem handling into a method that can be re-used in different routing trees:

```java
Success success = rest.execute(GET, url)
        .dispatch(series(),
                on(SUCCESSFUL).capture(Success.class),
                on(CLIENT_ERROR)
                    .dispatch(status(),
                            on(UNAUTHORIZED).call(this::login),
                            on(UNPROCESSABLE_ENTITY).dispatch(this::problems)),
                on(SERVER_ERROR)
                    .dispatch(statusCode(),
                            on(503).call(this::retryLater),
                anySeries().call(this::fail))
        .as(Success.class).orElse(null);

private Binding<HttpStatus> problems(final Condition<HttpStatus> condition) {
    return condition.dispatch(contentType(),
            on(PROBLEM).capture(Problem.class),
            on(ERROR).capture(Problem.class),
            anyContentType().call(this::fail));
}
```

#### Routing Templates

Response handling is sometimes pretty similar for many types of requests, and can often be handled using the same
routing logic. To enable reuse of routing logic, Riptide supports the creation of *routing templates*, that can be
used for dispatching any number of responses. The following example defines a nested hierarchy of routing templates to support similar expected behaviors:

```java
static final Router<MediaType> CONTENT_ROUTER = Router.create(contentType(),
        on(PERTNER).capture(Partner.class),
        on(CONTRACT).capture(Contract.class),
        on(PROBLEM).call(Problem.class, Example::propagate),
        on(ERROR).call(Error.class, Example::propagate),
        anyContentType().call(Example::failContent));

static final Router<HttpStatus> STATUS_ROUTER = Router.create(status(),
        on(OK).dispatch(CONTENT_ROUTER),
        anyStatus().call(Example::failStatus));

static final Router<HttpStatus.Series> SERIES_ROUTER = Router.create(series(),
        on(SUCCESSFUL).dispatch(CONTENT_ROUTER),
        on(CLIENT_ERROR).dispatch(CONTENT_ROUTER),
        on(SERVER_ERROR).dispatch(STATUS_ROUTER),
        anySeries().call(Example::failStatus));
```

These templates can be used and adapted as follows to support a retry mechanism on internal server errors:

```java
Rest rest = Rest.create(new RestTemplate());
Router<HttpStatus> router = STATUS_ROUTER.add(on(INTERNAL_SERVER_ERROR).call(this::retryLater));
Partner partner = rest.execute(HttpMethod.GET, URI.create("http://example.com/api"))
           .dispatch(SERIES_ROUTER.add(on(SERVER_ERROR).dispatch(router)))
           .as(Partner.class);

```

Warning: be careful that all methods and instances referenced by the routing template are thread safe.

### Patterns and Examples

This section contains some ready to be used patterns and examples on how to solve certain challenges using Riptide:

#### Follow Redirects

```java
private void send(URI url, T body) {
    rest.execute(POST, url, body).dispatch(series(),
            on(SUCCESSFUL).call(pass()),
            on(REDIRECTION).call(response ->
                    send(response.getHeaders().getLocation(), body)),
            anySeries().call(this::fail));
}
```

#### Create resource and retrieve location

```java
private URI create(URI url, T body) {
    return rest.execute(POST, url, body).dispatch(series(),
            on(SUCCESSFUL).capture(location()),
            anySeries().call(this::fail))
            .to(URI.class);
}
```

#### Create resource and retrieve its current state

```java
private String create(URI url, String body) {
    return unit.execute(POST, url, body).dispatch(series(),
            on(SUCCESSFUL).dispatch(resolveAgainst(url), isCurrentRepresentation(),
                    on(true).capture(String.class),
                    on(false).capture(location().andThen(location ->
                            unit.execute(GET, location).dispatch(series(),
                                    on(SUCCESSFUL).capture(String.class),
                                    anySeries().call(this::fail))
                                    .to(String.class)))),
            anySeries().call(this::fail))
            .to(String.class);
}
```

### Exceptions

*Riptide* propagates any exception thrown by the underlying `RestTemplate` or any of the custom callbacks passed to
`call` or `capture` *as-is*, which means if you're interested in any of those, you can put the call to `Rest.execute(..)`
in a `try-catch` and directly catch it. When using `AsyncRest` a traditional `try-catch` wouldn't work, since the
exception will be thrown in another thread. You can either retrieve the exception upon calling `Future.get(..)`:

```java
try {
    rest.execute(GET, url).dispatch(..).get(10, SECONDS);
} catch (final ExecutionException e) {
    // TODO implement
}
```

or alternatively register a callback for handling the exception asynchronously:

```java
rest.execute(GET, url).dispatch(..)
        .addCallback(handle(e -> {
            // TODO implement
        }));
```

The only special custom exception you may get is `NoRouteException`, if and only if there was no matching condition and
no wildcard condition either.

## Getting help

If you have questions, concerns, bug reports, etc, please file an issue in this repository's Issue Tracker.

## Getting involved

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change.
For more details check the [contribution guidelines](CONTRIBUTING.md).

## Credits and references

- [URL routing](http://littledev.nl/?p=99)
