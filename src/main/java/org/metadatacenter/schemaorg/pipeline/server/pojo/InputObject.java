package org.metadatacenter.schemaorg.pipeline.server.pojo;

import static com.google.common.base.Preconditions.checkNotNull;
import javax.annotation.Nonnull;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InputObject {

  private Mapping mapping;
  private DataSource dataSource;

  public InputObject() {
    // Needed by Jackson deserialization
  }

  public InputObject(@Nonnull Mapping mapping, @Nonnull DataSource dataSource) {
    this.mapping = checkNotNull(mapping);
    this.dataSource = checkNotNull(dataSource);
  }

  @JsonProperty
  public Mapping getMapping() {
    return mapping;
  }

  @JsonProperty
  public void setMapping(Mapping mapping) {
    this.mapping = mapping;
  }

  @JsonProperty
  public DataSource getDataSource() {
    return dataSource;
  }

  @JsonProperty
  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }
}
