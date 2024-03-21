package services.openapi;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class OpenApiSchemaSettings {
  public abstract String getBaseUrl();

  public abstract String getItEmailAddress();

  public abstract Boolean getAllowHttpScheme();

  public static OpenApiSchemaSettings.Builder builder() {
    return new AutoValue_OpenApiSchemaSettings.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract OpenApiSchemaSettings.Builder setBaseUrl(String baseUrl);

    public abstract OpenApiSchemaSettings.Builder setItEmailAddress(String itEmailAddress);

    public abstract OpenApiSchemaSettings.Builder setAllowHttpScheme(Boolean allowHttpScheme);

    public abstract OpenApiSchemaSettings build();
  }
}
