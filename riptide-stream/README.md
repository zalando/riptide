# Riptide: Streams Extension


*Riptide Stream* extension allows to capture arbitrary infinite object streams via Spring's [RestTemplate](https://spring.io/guides/gs/consuming-rest/). This includes infinite streaming format as application/x-json-stream and application/json-seq, but also streaming of simple finite lists/arrays of JSON objects. To enable streaming you only need to register the (stream converter)[src/main/java/org/zalando/riptide/stream/StreamConverter.java] with Riptide and declare a route for your stream that is calling a the stream consumer as follows:


```java
@JsonAutoDetect(fieldVisibility = NON_PRIVATE)
static class Contributor {
    String login;
    int contributions;
}

public static void main(final String... args) {
    try (Rest rest = Rest.builder().baseUrl("https://api.github.com").converter(streamConverter()).build()) {
        rest.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .accept(MediaType.APPLICATION_JSON)
                .dispatch(series(),
                       on(SUCCESSFUL).call(streamOf(User.class),
                               forEach(user -> println(user.login + " (" + user.contributions + ")"))))
                .get();
    }
}

The unique entry point for all specific methods is the (Streams)[src/main/java/org/zalando/riptide/stream/Streams.java] class. *Note:* The stream converter is an replacement to the default spring JSON converter that does not support streaming, and thus should be not registered together with it.
```
