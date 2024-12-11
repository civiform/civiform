package services.openapi;

import services.openapi.v2.Swagger2SchemaGenerator;

/** Factory for selecting the correct openapi schema generator based on openapi version */
public final class OpenApiSchemaGeneratorFactory {
  public static OpenApiSchemaGenerator createGenerator(
      OpenApiVersion openApiVersion, OpenApiSchemaSettings openApiSchemaSettings) {
    switch (openApiVersion) {
      case SWAGGER_V2 -> {
        return new Swagger2SchemaGenerator(openApiSchemaSettings);
      }
      default -> throw new RuntimeException(String.format("'%s' is not supported", openApiVersion));
    }
  }
}
