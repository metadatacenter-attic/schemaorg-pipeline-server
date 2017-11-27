package org.metadatacenter.schemaorg.pipeline.server.pojo;

import java.util.List;
import jersey.repackaged.com.google.common.collect.Lists;

public class DataSourceTypes {

  public static final String SPARQL_ENDPOINT = "sparql-endpoint";
  public static final String XML = "xml";

  public static final List<String> SUPPORTED_TYPES = Lists.newArrayList();
  static {
    SUPPORTED_TYPES.add(SPARQL_ENDPOINT);
    SUPPORTED_TYPES.add(XML);
  }
}
