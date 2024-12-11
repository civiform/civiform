package services.openapi;

public record OpenApiSchemaSettings(
    String baseUrl, String itEmailAddress, Boolean allowHttpScheme) {}
