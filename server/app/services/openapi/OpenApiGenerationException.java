package services.openapi;

public class OpenApiGenerationException extends RuntimeException {
  public OpenApiGenerationException(OpenApiVersion openApiVersion, String errorMessage) {
    super(String.format("%s. OpenApiVersion: \"%s\".", errorMessage, openApiVersion));
  }

  public OpenApiGenerationException(
      OpenApiVersion openApiVersion, String errorMessage, Exception ex) {
    super(String.format("%s. OpenApiVersion: \"%s\".", errorMessage, openApiVersion), ex);
  }
}
