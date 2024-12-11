package services.openapi;

import services.program.ProgramDefinition;

/** Common interface for generating OpenApi schema based on a defined program. */
public interface OpenApiSchemaGenerator {
  String createSchema(ProgramDefinition programDefinition);
}
