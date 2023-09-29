package services.openApi.v3;

import static com.google.common.base.Preconditions.checkNotNull;

import services.openApi.OpenApiSchemaGenerator;
import services.openApi.OpenApiSchemaSettings;
import services.program.ProgramDefinition;

public class OpenApi3SchemaGenerator implements OpenApiSchemaGenerator {
  private final OpenApiSchemaSettings openApiSchemaSettings;

  public OpenApi3SchemaGenerator(OpenApiSchemaSettings openApiSchemaSettings) {
    this.openApiSchemaSettings = checkNotNull(openApiSchemaSettings);
  }

  @Override
  public String createSchema(ProgramDefinition programDefinition) {
    throw new UnsupportedOperationException(
        "OpenApi v3 is not yet implemented." + openApiSchemaSettings.getBaseUrl());
  }
}
