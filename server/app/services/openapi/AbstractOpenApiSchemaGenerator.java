package services.openapi;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import io.swagger.models.Scheme;

public abstract class AbstractOpenApiSchemaGenerator {
  protected final OpenApiSchemaSettings openApiSchemaSettings;

  protected AbstractOpenApiSchemaGenerator(OpenApiSchemaSettings openApiSchemaSettings) {
    this.openApiSchemaSettings = checkNotNull(openApiSchemaSettings);
  }

  /**
   * Returns the list of schemes to allow in the swagger schema. Special case to allow http for
   * non-prod environments for testing purposes.
   */
  protected ImmutableList<Scheme> getSchemes() {
    if (openApiSchemaSettings.allowHttpScheme()) {
      return ImmutableList.of(Scheme.HTTP, Scheme.HTTPS);
    }

    return ImmutableList.of(Scheme.HTTPS);
  }

  /** Gets the baseurl without scheme */
  protected String getHostName() {
    return openApiSchemaSettings.baseUrl().replace("https://", "").replace("http://", "");
  }
}
