package org.metadatacenter.schemaorg.pipeline.server;

import org.hibernate.validator.constraints.NotEmpty;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

public class PipelineServerConfiguration extends Configuration {

  @NotEmpty
  private String serviceName = "Schema.org Pipeline Service";

  @NotEmpty
  private String serviceVersion;

  @JsonProperty
  public void setServiceName(String name) {
      this.serviceName = name;
  }

  @JsonProperty
  public String getDefaultName() {
      return serviceName;
  }

  @JsonProperty
  public void setServiceVersion(String version) {
      this.serviceVersion = version;
  }

  @JsonProperty
  public String getServiceVersion() {
      return serviceVersion;
  }
}