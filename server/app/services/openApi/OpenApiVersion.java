package services.openApi;

import java.util.Optional;

public enum OpenApiVersion {
  SWAGGER_V2(VersionLabels.swagger_v2),
  OPENAPI_V3(VersionLabels.openapi_v3);

  // Nest class is here as a hack/workaround because you can't put the strings directly in the enum
  // due to when they get initialized
  private static final class VersionLabels {
    private static final String swagger_v2 = "swagger-v2";
    private static final String openapi_v3 = "openapi-v3";
  }

  public static OpenApiVersion fromStringOrDefault(Optional<String> openApiVersion) {
    switch (openApiVersion.orElse(SWAGGER_V2.toString())) {
      case VersionLabels.openapi_v3:
        return OPENAPI_V3;
      case VersionLabels.swagger_v2:
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
