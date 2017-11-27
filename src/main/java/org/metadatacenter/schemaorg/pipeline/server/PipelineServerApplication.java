package org.metadatacenter.schemaorg.pipeline.server;

import org.metadatacenter.schemaorg.pipeline.server.health.OperationHealthCheck;
import org.metadatacenter.schemaorg.pipeline.server.resources.OperationResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class PipelineServerApplication extends Application<PipelineServerConfiguration> {

  private static final Logger logger = LoggerFactory.getLogger(PipelineServerApplication.class);

  public static void main(String[] args) throws Exception {
    new PipelineServerApplication().run(args);
  }

  @Override
  public String getName() {
    return "schemaorg-pipeline-server";
  }

  @Override
  public void initialize(Bootstrap<PipelineServerConfiguration> bootstrap) {
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
  }

  @Override
  public void run(PipelineServerConfiguration configuration, Environment environment)
      throws Exception {
    logger.info("Registering pipeline operations");
    environment.jersey().register(new OperationResource());
    OperationHealthCheck healthCheck = new OperationHealthCheck(configuration.getServiceVersion());
    environment.healthChecks().register("operation-health-check", healthCheck);
  }
}
