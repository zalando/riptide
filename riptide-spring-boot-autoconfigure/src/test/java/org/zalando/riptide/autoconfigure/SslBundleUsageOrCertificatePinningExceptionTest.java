package org.zalando.riptide.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class SslBundleUsageOrCertificatePinningExceptionTest {

  @Test
  void initialize() {
    SslBundleUsageOrCertificatePinningException ex = new SslBundleUsageOrCertificatePinningException("test-client-id");
    assertEquals("test-client-id", ex.getClientId());
  }

  @Test
  void makeJacocoHappy() {
    SslBundleUsageOrCertificatePinningException ex = new SslBundleUsageOrCertificatePinningException("test-client-id", "test message");
    assertEquals("test message", ex.getMessage());
    RuntimeException re = new RuntimeException();
    ex = new SslBundleUsageOrCertificatePinningException("test-client-id", "test message", re);
    assertEquals("test message", ex.getMessage());
    assertEquals(re, ex.getCause());
  }

  @Test
  void checkFailureAnalyzerMessages() {
    SslBundleUsageOrCertificatePinningFailureAnalyzer a = new SslBundleUsageOrCertificatePinningFailureAnalyzer();
    FailureAnalysis analysis = a.analyze(null, new SslBundleUsageOrCertificatePinningException("test-client-id"));
    assertEquals(analysis.getDescription(), "The http-client 'test-client-id' is configured to use CertificatePinning and SslBundleUsage at the same time.");
    assertEquals(analysis.getAction(), "Configure only CertificatePinning or SslBundleUsage for http-client 'test-client-id'");
  }

}
