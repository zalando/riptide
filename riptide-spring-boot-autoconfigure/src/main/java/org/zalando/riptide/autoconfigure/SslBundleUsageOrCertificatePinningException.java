package org.zalando.riptide.autoconfigure;

import org.springframework.core.NestedRuntimeException;

public class SslBundleUsageOrCertificatePinningException extends NestedRuntimeException {

  private final String clientId;

  public SslBundleUsageOrCertificatePinningException(final String clientId) {
    super(createMessage(clientId));
    this.clientId = clientId;
  }

  public SslBundleUsageOrCertificatePinningException(String clientId, String msg) {
    super(msg);
    this.clientId = clientId;
  }

  public SslBundleUsageOrCertificatePinningException(String clientId, String msg, Throwable cause) {
    super(msg, cause);
    this.clientId = clientId;
  }

  protected static String createMessage(String clientId) {
    return String.format("CertificatePinning and SslBundleUsage configured at same time in http-client : '%s'", clientId);
  }

  public String getClientId() {
    return clientId;
  }
}
