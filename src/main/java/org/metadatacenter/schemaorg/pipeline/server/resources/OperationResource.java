package org.metadatacenter.schemaorg.pipeline.server.resources;

import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.metadatacenter.schemaorg.pipeline.Pipeline;
import org.metadatacenter.schemaorg.pipeline.experimental.BioPortalRecommender;
import org.metadatacenter.schemaorg.pipeline.experimental.DBpediaLookup;
import org.metadatacenter.schemaorg.pipeline.experimental.SchemaEnrichment;
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
import org.metadatacenter.schemaorg.pipeline.server.pojo.InputObject;
import org.metadatacenter.schemaorg.pipeline.server.pojo.Mapping;
import org.metadatacenter.schemaorg.pipeline.server.pojo.MappingLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

@Path("/pipeline")
public class OperationResource {

  private static final Logger logger = LoggerFactory.getLogger(OperationResource.class);
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final ObjectWriter jsonWriter = mapper.writerWithDefaultPrettyPrinter();

  @POST
  @Timed
  @Path("/end2end")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response all(InputObject ino) {
    try {
      logger.info("POST /pipeline/end2end\n{}" ,jsonWriter.writeValueAsString(ino));
      final Mapping mapping = checkMappingValid(ino.getMapping());
      final DataSource dataSource = checkDataSourceValid(ino.getDataSource());
      final String dataSourceType = dataSource.getType();
      Map<String, String> output = Maps.newLinkedHashMap();
      if (dataSourceType.equals(DataSourceTypes.SPARQL_ENDPOINT)) {
        useSparqlPipeline(mapping, dataSource, output);
      } else if (dataSourceType.equals(DataSourceTypes.XML)) {
        useXmlPipeline(mapping, dataSource, output);
      }
      return Response.status(Status.OK).entity(mapper.writeValueAsString(output)).build();
    } catch (Exception e) {
      String errorMessage = toJsonErrorMessage(Status.BAD_REQUEST.getStatusCode(), e.getMessage());
      logger.error(errorMessage);
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .entity(errorMessage).build();
    }
  }

  private void useSparqlPipeline(Mapping mapping, DataSource dataSource, final Map<String, String> output) {
    String sparqlQuery = translateToSparql(mapping);
    output.put("query", sparqlQuery);
    String rdfDocument = new SparqlEndpointClient(dataSource.getValue()).evaluate(sparqlQuery);
    String schemaOutput = RdfToSchema.transform(rdfDocument);
    schemaOutput = SchemaEnrichment.fillOutIdFromObjectName(schemaOutput, new DBpediaLookup());
    schemaOutput = SchemaEnrichment.fillOutIdFromObjectCodeValue(schemaOutput, new BioPortalRecommender());
    output.put("schema", schemaOutput);
    String htmlOutput = SchemaToHtml.transform(schemaOutput);
    output.put("html", htmlOutput);
  }

  private void useXmlPipeline(Mapping mapping, DataSource dataSource, final Map<String, String> output) {
    String stylesheet = translateToXslt(mapping);
    output.put("query", stylesheet);
    String xmlOutput = XsltTransformer.newTransformer(stylesheet).transform(dataSource.getValue());
    String schemaOutput = XmlToSchema.transform(xmlOutput);
    schemaOutput = SchemaEnrichment.fillOutIdFromObjectName(schemaOutput, new DBpediaLookup());
    schemaOutput = SchemaEnrichment.fillOutIdFromObjectCodeValue(schemaOutput, new BioPortalRecommender());
    output.put("schema", schemaOutput);
    String htmlOutput = SchemaToHtml.transform(schemaOutput);
    output.put("html", htmlOutput);
  }

  @POST
  @Timed
  @Path("/map2sparql")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  public Response toSparql(InputObject ino) {
    try {
      logger.info("POST /pipeline/map2sparql\n{}" ,jsonWriter.writeValueAsString(ino));
      final Mapping mapping = checkMappingValid(ino.getMapping());
      String output = translateToSparql(mapping);
      return Response.status(Status.OK).entity(output).build();
    } catch (Exception e) {
      String errorMessage = toJsonErrorMessage(Status.BAD_REQUEST.getStatusCode(), e.getMessage());
      logger.error(errorMessage);
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .entity(errorMessage).build();
    }
  }

  @POST
  @Timed
  @Path("/map2xslt")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  public Response toXslt(InputObject ino) {
    try {
      logger.info("POST /pipeline/map2xslt\n{}" ,jsonWriter.writeValueAsString(ino));
      final Mapping mapping = checkMappingValid(ino.getMapping());
      String output = translateToXslt(mapping);
      return Response.status(Status.OK).entity(output).build();
    } catch (Exception e) {
      String errorMessage = toJsonErrorMessage(Status.BAD_REQUEST.getStatusCode(), e.getMessage());
      logger.error(errorMessage);
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .entity(errorMessage).build();
    }
  }

  @POST
  @Timed
  @Path("/map2query")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  public Response toQueryLanguage(InputObject ino) {
    try {
      logger.info("POST /pipeline/map2query\n{}" ,jsonWriter.writeValueAsString(ino));
      final Mapping mapping = checkMappingValid(ino.getMapping());
      final DataSource dataSource = checkDataSourceValid(ino.getDataSource());
      final String dataSourceType = dataSource.getType();
      String output = "";
      if (dataSourceType.equals(DataSourceTypes.SPARQL_ENDPOINT)) {
        output = translateToSparql(mapping);
      } else if (dataSourceType.equals(DataSourceTypes.XML)) {
        output = translateToXslt(mapping);
      }
      return Response.status(Status.OK).entity(output).build();
    } catch (Exception e) {
      String errorMessage = toJsonErrorMessage(Status.BAD_REQUEST.getStatusCode(), e.getMessage());
      logger.error(errorMessage);
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .entity(errorMessage).build();
    }
  }

  @POST
  @Timed
  @Path("/data2schema")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response transform(InputObject ino) {
    try {
      logger.info("POST /pipeline/data2schema\n{}" ,jsonWriter.writeValueAsString(ino));
      final Mapping mapping = checkMappingValid(ino.getMapping());
      final DataSource dataSource = checkDataSourceValid(ino.getDataSource());
      final String dataSourceType = dataSource.getType();
      final String dataSourceValue = dataSource.getValue();
      String output = "{}";
      if (dataSourceType.equals(DataSourceTypes.SPARQL_ENDPOINT)) {
        String sparqlQuery = translateToSparql(mapping);
        SparqlEndpointClient endpointClient = new SparqlEndpointClient(dataSourceValue);
        output = Pipeline.create()
            .pipe(s -> endpointClient.evaluate(s))
            .pipe(RdfToSchema::transform)
            .pipe(s -> SchemaEnrichment.fillOutIdFromObjectName(s, new DBpediaLookup()))
            .pipe(s -> SchemaEnrichment.fillOutIdFromObjectCodeValue(s, new BioPortalRecommender()))
            .run(sparqlQuery);
      } else if (dataSourceType.equals(DataSourceTypes.XML)) {
        String stylesheet = translateToXslt(mapping);
        XsltTransformer transformer = XsltTransformer.newTransformer(stylesheet);
        output = Pipeline.create()
            .pipe(transformer::transform)
            .pipe(XmlToSchema::transform)
            .pipe(s -> SchemaEnrichment.fillOutIdFromObjectName(s, new DBpediaLookup()))
            .pipe(s -> SchemaEnrichment.fillOutIdFromObjectCodeValue(s, new BioPortalRecommender()))
            .run(dataSourceValue);
      }
      return Response.status(Status.OK).entity(output).build();
    } catch (Exception e) {
      String errorMessage = toJsonErrorMessage(Status.BAD_REQUEST.getStatusCode(), e.getMessage());
      logger.error(errorMessage);
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .entity(errorMessage).build();
    }
  }

  @POST
  @Timed
  @Path("/data2html")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_HTML)
  public Response embed(InputObject ino) {
    try {
      logger.info("POST /pipeline/data2html\n{}" ,jsonWriter.writeValueAsString(ino));
      final Mapping mapping = checkMappingValid(ino.getMapping());
      final DataSource dataSource = checkDataSourceValid(ino.getDataSource());
      final String dataSourceType = dataSource.getType();
      final String dataSourceValue = dataSource.getValue();
      String output = "{}";
      if (dataSourceType.equals(DataSourceTypes.SPARQL_ENDPOINT)) {
        String sparqlQuery = translateToSparql(mapping);
        SparqlEndpointClient endpointClient = new SparqlEndpointClient(dataSourceValue);
        output = Pipeline.create()
            .pipe(s -> endpointClient.evaluate(s))
            .pipe(RdfToSchema::transform)
            .pipe(s -> SchemaEnrichment.fillOutIdFromObjectName(s, new DBpediaLookup()))
            .pipe(s -> SchemaEnrichment.fillOutIdFromObjectCodeValue(s, new BioPortalRecommender()))
            .pipe(SchemaToHtml::transform)
            .run(sparqlQuery);
      } else if (dataSourceType.equals(DataSourceTypes.XML)) {
        String stylesheet = translateToXslt(mapping);
        XsltTransformer transformer = XsltTransformer.newTransformer(stylesheet);
        output = Pipeline.create()
            .pipe(transformer::transform)
            .pipe(XmlToSchema::transform)
            .pipe(s -> SchemaEnrichment.fillOutIdFromObjectName(s, new DBpediaLookup()))
            .pipe(s -> SchemaEnrichment.fillOutIdFromObjectCodeValue(s, new BioPortalRecommender()))
            .pipe(SchemaToHtml::transform)
            .run(dataSourceValue);
      }
      return Response.status(Status.OK).entity(output).build();
    } catch (Exception e) {
      String errorMessage = toJsonErrorMessage(Status.BAD_REQUEST.getStatusCode(), e.getMessage());
      logger.error(errorMessage);
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .entity(errorMessage).build();
    }
  }

  private String translateToSparql(final Mapping mapping) {
    TranslatorHandler handler = new SparqlConstructTranslatorHandler();
    String output = MapNodeTranslator.translate(handler, mapping.getValue(), mapping.getLanguage());
    return output;
  }

  private String translateToXslt(final Mapping mapping) {
    TranslatorHandler handler = new XsltTranslatorHandler();
    String output = MapNodeTranslator.translate(handler, mapping.getValue(), mapping.getLanguage());
    return output;
  }

  /*
   * Some utility methods
   */

  private static String toJsonErrorMessage(int responseCode, String message) {
    return String.format("{ \"responseCode\": \"%s\", \"message\": \"%s\" }", responseCode, message);
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
    if (Strings.isNullOrEmpty(language)) {
      String errorMessage = String.format(
          "Language selection is empty, possible values: %s",
          MappingLanguages.SUPPORTED_LANGUAGES);
      throw new Exception(errorMessage);
    }
    if (!MappingLanguages.SUPPORTED_LANGUAGES.contains(language)) {
      String errorMessage = String.format(
          "Invalid language selection (language: %s), must be the following: %s",
          language, MappingLanguages.SUPPORTED_LANGUAGES);
      throw new Exception(errorMessage);
    }
  }

  private static void checkTypeValid(String type) throws Exception {
    if (Strings.isNullOrEmpty(type)) {
      String errorMessage = String.format(
          "Data source selection is empty, possible values: %s",
          MappingLanguages.SUPPORTED_LANGUAGES);
      throw new Exception(errorMessage);
    }
    if (!DataSourceTypes.SUPPORTED_TYPES.contains(type)) {
      String errorMessage = String.format(
          "Invalid data source selection (type: %s), must be the following: %s",
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
