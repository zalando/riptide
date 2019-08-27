package org.zalando.riptide;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.test.web.client.*;

import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

// this test is just a compile time check for checked exceptions (throws clauses)
@ExtendWith(MockitoExtension.class)
final class ExceptionHandlingTest {

    private final Http unit;
    private final MockRestServiceServer server;

    @Mock
    private RoutingTree<Void> tree;

    ExceptionHandlingTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getHttp();
        this.server = setup.getServer();
    }

    @BeforeEach
    void setUp() {
        server.expect(requestTo("https://api.example.com/"))
                .andRespond(withSuccess());
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void shouldNotThrowIOExceptionWhenSettingBody() {
        unit.get("/").body("body").call(tree).join();
    }

    @Test
    void shouldNotThrowIOExceptionWhenDispatchingWithoutBody() {
        unit.get("/").call(tree).join();
    }

    @Test
    void shouldNotThrowInterruptedAndExecutionExceptionWhenBlocking() {
        unit.get("/").dispatch(tree).join();
    }

    @Test
    void shouldThrowInterruptedExecutionAndTimeoutExceptionWhenBlocking() throws InterruptedException,
            ExecutionException, TimeoutException {

        unit.get("/")
                .body("")
                .call(tree).get(10, SECONDS);
    }

}
