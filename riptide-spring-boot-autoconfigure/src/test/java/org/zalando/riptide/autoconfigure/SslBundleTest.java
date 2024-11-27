package org.zalando.riptide.autoconfigure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.riptide.Http;

@SpringBootTest(classes = {SslBundleTestConfiguration.class})
@ActiveProfiles("ssl-bundle")
public class SslBundleTest {

  @Autowired
  @Qualifier("ssl-bundle-test")
  private Http sslBundleHttp;

  @Test
  void shouldAutowireHttp() {
    assertThat(sslBundleHttp, is(notNullValue()));
  }

}
