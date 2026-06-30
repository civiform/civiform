package services.openapi;

import services.openapi.v2.Swagger2SchemaGenerator;
import services.openapi.v3.OpenApi3SchemaGenerator;

/** Factory for selecting the correct openapi schema generator based on openapi version */
public final class OpenApiSchemaGeneratorFactory {
  public static OpenApiSchemaGenerator createGenerator(
      OpenApiVersion openApiVersion, OpenApiSchemaSettings openApiSchemaSettings) {
    return switch (openApiVersion) {
      case SWAGGER_V2 -> new Swagger2SchemaGenerator(openApiSchemaSettings);
      case OPENAPI_V3_0 -> new OpenApi3SchemaGenerator(openApiSchemaSettings);
    };
  }
}
