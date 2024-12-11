package services.openapi;

/** Common settings used when building an OpenApi schema */
public record OpenApiSchemaSettings(
    String baseUrl, String itEmailAddress, Boolean allowHttpScheme) {}
