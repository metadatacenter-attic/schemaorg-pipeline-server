package org.metadatacenter.schemaorg.pipeline.server.resources;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.metadatacenter.schemaorg.pipeline.Pipeline;
import org.metadatacenter.schemaorg.pipeline.operation.embed.SchemaToHtml;
import org.metadatacenter.schemaorg.pipeline.operation.extract.SparqlEndpointClient;
import org.metadatacenter.schemaorg.pipeline.operation.extract.XsltTransformer;
import org.metadatacenter.schemaorg.pipeline.operation.transform.RdfToSchema;
import org.metadatacenter.schemaorg.pipeline.operation.transform.XmlToSchema;
import org.metadatacenter.schemaorg.pipeline.operation.translate.MapNodeTranslator;
import org.metadatacenter.schemaorg.pipeline.operation.translate.SparqlConstructTranslatorHandler;
import org.metadatacenter.schemaorg.pipeline.operation.translate.TranslatorHandler;
import org.metadatacenter.schemaorg.pipeline.operation.translate.XsltTranslatorHandler;
import org.metadatacenter.schemaorg.pipeline.server.pojo.DataSource;
import org.metadatacenter.schemaorg.pipeline.server.pojo.DataSourceTypes;
import org.metadatacenter.schemaorg.pipeline.server.pojo.Mapping;
import org.metadatacenter.schemaorg.pipeline.server.pojo.MappingLanguages;
import org.metadatacenter.schemaorg.pipeline.server.pojo.InputObject;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

@Path("/operations")
public class OperationResource {

  private static final String PARAM_TO = "to";
  private static final List<String> TRANSLATION_OPTIONS = Lists.newArrayList();
  static {
    TRANSLATION_OPTIONS.add("sparql");
    TRANSLATION_OPTIONS.add("xslt");
  }

  @POST
  @Timed
  @Path("/translate")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  public Response translate(@QueryParam(PARAM_TO) String value, InputObject ino) {
    try {
      checkArgument(value);
      final Mapping mapping = checkMappingValid(ino.getMapping());
      TranslatorHandler handler = getTranslationHandler(value);
      String output = MapNodeTranslator.translate(handler, mapping.getValue(), mapping.getLanguage());
      return Response.status(Status.OK).entity(output).build();
    } catch (Exception e) {
      String errorMessage = toJsonErrorMessage(Status.BAD_REQUEST.getStatusCode(), e.getMessage());
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .entity(errorMessage).build();
    }
  }

  @POST
  @Timed
  @Path("/transform")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response transform(InputObject ino) {
    try {
      final Mapping mapping = checkMappingValid(ino.getMapping());
      final DataSource dataSource = checkDataSourceValid(ino.getDataSource());
      final String mappingLanguage = mapping.getLanguage();
      final String mappingText = mapping.getValue();
      final String dataSourceType = dataSource.getType();
      final String dataSourceValue = dataSource.getValue();
      String output = "{}";
      if (dataSourceType.equals(DataSourceTypes.SPARQL_ENDPOINT)) {
        TranslatorHandler handler = new SparqlConstructTranslatorHandler();
        String sparqlQuery = MapNodeTranslator.translate(handler, mappingText, mappingLanguage);
        SparqlEndpointClient endpointClient = new SparqlEndpointClient(dataSourceValue);
        output = Pipeline.create()
            .pipe(s -> endpointClient.evaluatePreparedQuery(s))
            .pipe(RdfToSchema::transform)
            .run(sparqlQuery);
      } else if (dataSourceType.equals(DataSourceTypes.XML)) {
        TranslatorHandler handler = new XsltTranslatorHandler();
        String stylesheet = MapNodeTranslator.translate(handler, mappingText, mappingLanguage);
        XsltTransformer transformer = XsltTransformer.newTransformer(stylesheet);
        output = Pipeline.create()
            .pipe(transformer::transform)
            .pipe(XmlToSchema::transform)
            .run(dataSourceValue);
      }
      return Response.status(Status.OK).entity(output).build();
    } catch (Exception e) {
      String errorMessage = toJsonErrorMessage(Status.BAD_REQUEST.getStatusCode(), e.getMessage());
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .entity(errorMessage).build();
    }
  }

  @POST
  @Timed
  @Path("/embed")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_HTML)
  public Response embed(InputObject ino) {
    try {
      final Mapping mapping = checkMappingValid(ino.getMapping());
      final DataSource dataSource = checkDataSourceValid(ino.getDataSource());
      final String mappingLanguage = mapping.getLanguage();
      final String mappingText = mapping.getValue();
      final String dataSourceType = dataSource.getType();
      final String dataSourceValue = dataSource.getValue();
      String output = "{}";
      if (dataSourceType.equals(DataSourceTypes.SPARQL_ENDPOINT)) {
        TranslatorHandler handler = new SparqlConstructTranslatorHandler();
        String sparqlQuery = MapNodeTranslator.translate(handler, mappingText, mappingLanguage);
        SparqlEndpointClient endpointClient = new SparqlEndpointClient(dataSourceValue);
        output = Pipeline.create()
            .pipe(s -> endpointClient.evaluatePreparedQuery(s))
            .pipe(RdfToSchema::transform)
            .pipe(SchemaToHtml::transform)
            .run(sparqlQuery);
      } else if (dataSourceType.equals(DataSourceTypes.XML)) {
        TranslatorHandler handler = new XsltTranslatorHandler();
        String stylesheet = MapNodeTranslator.translate(handler, mappingText, mappingLanguage);
        XsltTransformer transformer = XsltTransformer.newTransformer(stylesheet);
        output = Pipeline.create()
            .pipe(transformer::transform)
            .pipe(XmlToSchema::transform)
            .pipe(SchemaToHtml::transform)
            .run(dataSourceValue);
      }
      return Response.status(Status.OK).entity(output).build();
    } catch (Exception e) {
      String errorMessage = toJsonErrorMessage(Status.BAD_REQUEST.getStatusCode(), e.getMessage());
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .entity(errorMessage).build();
    }
  }

  private TranslatorHandler getTranslationHandler(String name) throws Exception {
    TranslatorHandler handler = null;
    if ("sparql".equals(name)) {
      handler = new SparqlConstructTranslatorHandler();
    } else if ("xslt".equals(name)) {
      handler = new XsltTranslatorHandler();
    }
    return handler;
  }

  /*
   * Some utility methods
   */

  private static String toJsonErrorMessage(int responseCode, String message) {
    return String.format("{ \"responseCode\": \"%s\", \"message\": \"%s\" }", responseCode, message);
  }

  private static void checkArgument(String parameterValue) throws Exception {
    if (Strings.isNullOrEmpty(parameterValue)) {
      String errorMessage = String.format("Missing argument /translate?to=%s", TRANSLATION_OPTIONS);
      throw new Exception(errorMessage);
    }
    if (!TRANSLATION_OPTIONS.contains(parameterValue)) {
      String errorMessage = String.format("Invalid argument /translate?to=%s, must be the following: %s",
          parameterValue, TRANSLATION_OPTIONS);
      throw new Exception(errorMessage);
    }
  }

  private static Mapping checkMappingValid(Mapping mapping) throws Exception {
    try {
      checkLanguageValid(mapping.getLanguage());
      checkNotEmpty(mapping.getValue());
      return mapping;
    } catch (NullPointerException e) {
      throw new Exception("Missing the required 'mapping' object in the request body");
    }
  }

  private static DataSource checkDataSourceValid(DataSource dataSource) throws Exception {
    try {
      checkTypeValid(dataSource.getType());
      checkNotEmpty(dataSource.getValue());
      return dataSource;
    } catch (NullPointerException e) {
      throw new Exception("Missing the required 'dataSource' object in the request body");
    }
  }

  private static void checkLanguageValid(String language) throws Exception {
    if (!MappingLanguages.SUPPORTED_LANGUAGES.contains(language)) {
      String errorMessage = String.format(
          "Invalid field value \"language\": \"%s\", must be the following: %s",
          language, MappingLanguages.SUPPORTED_LANGUAGES);
      throw new Exception(errorMessage);
    }
  }

  private static void checkTypeValid(String type) throws Exception {
    if (!DataSourceTypes.SUPPORTED_TYPES.contains(type)) {
      String errorMessage = String.format(
          "Invalid field value \"type\": \"%s\", must be the following: %s",
          type, DataSourceTypes.SUPPORTED_TYPES);
      throw new Exception(errorMessage);
    }
  }

  private static void checkNotEmpty(String mappingText) throws Exception {
    if (Strings.isNullOrEmpty(mappingText)) {
      throw new Exception("The field value must not be null or empty");
    }
  }
}
