package services.openApi;

import services.program.ProgramDefinition;

public interface OpenApiSchemaGenerator {
  String createSchema(ProgramDefinition programDefinition);
}
