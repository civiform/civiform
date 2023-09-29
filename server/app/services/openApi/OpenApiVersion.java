package services.openApi;

import java.util.Optional;

public enum OpenApiVersion {
  SWAGGER_V2("swagger-v2"),
  OPENAPI_V3("openapi-v3");

  public static OpenApiVersion fromString(Optional<String> openApiVersion) {
    switch (openApiVersion.orElse("")) {
      case "openapi-v3":
        return OPENAPI_V3;
      case "swagger-v2":
        return SWAGGER_V2;
      default:
        throw new UnsupportedOperationException(
            String.format("'%s' is not a supported OpenAPI version.", openApiVersion));
    }
  }

  private final String name;

  OpenApiVersion(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
