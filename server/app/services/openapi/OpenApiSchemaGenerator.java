package services.openapi;

import services.program.ProgramDefinition;

public interface OpenApiSchemaGenerator {
  String createSchema(ProgramDefinition programDefinition);
}
