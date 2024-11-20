package org.zalando.riptide.autoconfigure.retry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.failsafe.RetryRoute.retry;

import com.google.common.base.Stopwatch;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;
import org.zalando.riptide.Http;
import org.zalando.riptide.autoconfigure.MetricsTestAutoConfiguration;
import org.zalando.riptide.autoconfigure.OpenTracingTestAutoConfiguration;
import org.zalando.riptide.autoconfigure.RiptideClientTest;

@RiptideClientTest
@ActiveProfiles("default")
public class XRateLimitResetRetryTest {
  @Configuration
  @ImportAutoConfiguration({
      JacksonAutoConfiguration.class,
      LogbookAutoConfiguration.class,
      OpenTracingTestAutoConfiguration.class,
      MetricsTestAutoConfiguration.class,
  })
  static class ContextConfiguration {
  }

  @Autowired
  @Qualifier("retry-test")
  private Http retryClient;

  @Autowired
  private MockRestServiceServer server;

  @Test
  void shouldObeyXRateLimitHeader() {
    server.expect(times(1), requestTo("http://retry-test"))
        .andRespond(withRawStatus(429).headers(new HttpHeaders() {{
          add("X-RateLimit-Limit", "1");
          add("X-RateLimit-Remaining", "0");
          add("X-RateLimit-Reset", "2");
        }}));
    server.expect(times(1), requestTo("http://retry-test")).andRespond(withSuccess());

    final Stopwatch stopwatch = Stopwatch.createStarted();
    retryClient.get()
        .dispatch(series(),
            on(SUCCESSFUL).call(pass()),
            anySeries().dispatch(status(),
                on(HttpStatus.TOO_MANY_REQUESTS).call(retry())))
        .join();
    Duration elapsed = stopwatch.stop().elapsed();

    assertThat(elapsed, greaterThanOrEqualTo(Duration.ofSeconds(2)));
    server.verify();
  }
}
