package services.openapi;

import java.util.Optional;

/** List of supported swagger/openapi versions */
public enum OpenApiVersion {
  SWAGGER_V2(VersionLabels.swagger_v2, "2.0"),
  OPENAPI_V3_0(VersionLabels.openapi_v3_0, "3.0");

  // Nest class is here as a hack/workaround because you can't put the strings directly in the enum
  // due to when they get initialized
  private static final class VersionLabels {
    private static final String swagger_v2 = "swagger-v2";
    private static final String openapi_v3_0 = "openapi-v3-0";
  }

  public static OpenApiVersion fromString(Optional<String> openApiVersion) {
    return switch (openApiVersion.orElse("")) {
      case VersionLabels.swagger_v2 -> SWAGGER_V2;
      case VersionLabels.openapi_v3_0 -> OPENAPI_V3_0;
      default ->
          throw new RuntimeException(
              String.format("OpenApiVersion %s is not supported", openApiVersion.orElse("")));
    };
  }

  private final String name;
  private final String versionNumber;

  OpenApiVersion(String name, String versionNumber) {
    this.name = name;
    this.versionNumber = versionNumber;
  }

  @Override
  public String toString() {
    return name;
  }

  public String getVersionNumber() {
    return versionNumber;
  }
}
