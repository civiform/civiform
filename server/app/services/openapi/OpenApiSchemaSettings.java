package services.openapi;

/** Common settings used when building an OpenApi schema */
public record OpenApiSchemaSettings(
    String baseUrl, String itEmailAddress, Boolean allowHttpScheme, Boolean includeScores) {

  /** Builds settings without answer-option score properties. */
  public OpenApiSchemaSettings(String baseUrl, String itEmailAddress, Boolean allowHttpScheme) {
    this(baseUrl, itEmailAddress, allowHttpScheme, /* includeScores= */ false);
  }
}
