package services.openApi;

import javax.transaction.NotSupportedException;
import services.openApi.v2.Swagger2SchemaGenerator;
import services.openApi.v3.OpenApi3SchemaGenerator;

public final class OpenApiSchemaGeneratorFactory {
  public static OpenApiSchemaGenerator createGenerator(
      OpenApiVersion openApiVersion, OpenApiSchemaSettings openApiSchemaSettings)
      throws NotSupportedException {
    switch (openApiVersion) {
      case SWAGGER_V2:
        return new Swagger2SchemaGenerator(openApiSchemaSettings);
      case OPENAPI_V3:
        return new OpenApi3SchemaGenerator(openApiSchemaSettings);
      default:
        throw new NotSupportedException(String.format("'%s' is not supported", openApiVersion));
    }
  }
}
