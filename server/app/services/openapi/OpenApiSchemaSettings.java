package services.openapi;

/**
 * Common settings used when building an OpenApi schema. {@code includeScores} is true when the
 * scoring flag is on and some version of the program used scoring.
 */
public record OpenApiSchemaSettings(
    String baseUrl, String itEmailAddress, Boolean allowHttpScheme, Boolean includeScores) {}
