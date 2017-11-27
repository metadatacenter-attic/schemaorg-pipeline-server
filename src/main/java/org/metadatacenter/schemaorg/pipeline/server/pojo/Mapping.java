package org.metadatacenter.schemaorg.pipeline.server.pojo;

import static com.google.common.base.Preconditions.checkNotNull;
import javax.annotation.Nonnull;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Mapping {

  private String language;
  private String value;

  public Mapping() {
    // Needed by Jackson deserialization
  }

  public Mapping(@Nonnull String language, @Nonnull String value) {
    this.language = checkNotNull(language);
    this.value = checkNotNull(value);
  }

  @JsonProperty
  public String getLanguage() {
    return language;
  }

  @JsonProperty
  public void setLanguage(String language) {
    this.language = language;
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
