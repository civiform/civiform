package services.openApi.v2;

/**
 * The location of the API key. Valid values are "query" or "header".
 *
 * <p>https://swagger.io/specification/v2/#security-scheme-object
 */
public enum ApiKeyLocation {
  QUERY("query"),
  HEADER("header");

  private final String name;

  ApiKeyLocation(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
