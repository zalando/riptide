package org.zalando.riptide.autoconfigure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class SslBundleUsageOrCertificatePinningFailureAnalyzer extends
    AbstractFailureAnalyzer<SslBundleUsageOrCertificatePinningException> {

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure,
      SslBundleUsageOrCertificatePinningException cause) {
    return new FailureAnalysis(getDescription(cause), getAction(cause), cause);
  }

  private String getDescription(SslBundleUsageOrCertificatePinningException cause) {
    return String.format("The http-client '%s' is configured to use CertificatePinning and SslBundleUsage at the same time.", cause.getClientId());
  }

  private String getAction(SslBundleUsageOrCertificatePinningException cause) {
    return String.format("Configure only CertificatePinning or SslBundleUsage for http-client '%s'", cause.getClientId());
  }
}
