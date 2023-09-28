package services.openApi;

import java.util.Optional;

public enum OpenApiVersion {
  SWAGGER_V2("swagger-v2"),
  OPENAPI_V3("openapi-v3");

  public static OpenApiVersion fromStringOrDefault(Optional<String> openApiVersion) {
    switch (openApiVersion.orElse("swagger-v2")) {
      case "openapi-v3":
        return OPENAPI_V3;
      case "swagger-v2":
      default:
        return SWAGGER_V2;
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
