package org.metadatacenter.schemaorg.pipeline.server.health;

import static com.google.common.base.Preconditions.checkNotNull;
import javax.annotation.Nonnull;
import com.codahale.metrics.health.HealthCheck;

public class OperationHealthCheck extends HealthCheck {

  private final String version;

  public OperationHealthCheck(@Nonnull String version) {
    this.version = checkNotNull(version);
  }
  
  @Override
  protected Result check() throws Exception {
    String message = String.format("Service is running healthy (version %s)", version);
    return Result.healthy(message);
  }
}
