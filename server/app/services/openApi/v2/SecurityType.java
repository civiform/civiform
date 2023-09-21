package services.openApi.v2;

/**
 * Field: type
 *
 * <p>Required. The type of the security scheme. Valid values are "basic", "apiKey" or "oauth2".
 *
 * <p>https://swagger.io/specification/v2/#security-scheme-object
 */
public enum SecurityType {
  BASIC("basic"),
  API_KEY("apiKey"),
//  OAUTH2("oauth2")
;

  private final String name;

  SecurityType(String name) {
    this.name = name;
  }

  /** Formatted label for a specific security type */
  public String getLabel() {
    return String.format("%sAuth", name);
  }

  @Override
  public String toString() {
    return name;
  }
}
