package org.metadatacenter.schemaorg.pipeline.server.pojo;

import static com.google.common.base.Preconditions.checkNotNull;
import javax.annotation.Nonnull;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DataSource {

  private String type;
  private String value;

  public DataSource() {
    // Needed by Jackson deserialization
  }

  public DataSource(@Nonnull String type, @Nonnull String value) {
    this.type = checkNotNull(type);
    this.value = checkNotNull(value);
  }

  @JsonProperty
  public String getType() {
    return type;
  }

  @JsonProperty
  public void setType(String type) {
    this.type = type;
  }

  @JsonProperty
  public String getValue() {
    return value;
  }

  @JsonProperty
  public void setValue(String value) {
    this.value = value;
  }
}
