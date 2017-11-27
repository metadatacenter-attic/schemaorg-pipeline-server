package org.metadatacenter.schemaorg.pipeline.server.pojo;

import java.util.List;
import jersey.repackaged.com.google.common.collect.Lists;

public class MappingLanguages {

  public static final List<String> SUPPORTED_LANGUAGES = Lists.newArrayList();
  static {
    SUPPORTED_LANGUAGES.add("caml");
    SUPPORTED_LANGUAGES.add("rml");
  }
}
