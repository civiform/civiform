package services.openApi.v2;

/**
 * https://swagger.io/specification/v2/#swagger-object
 *
 * <p>Field: schemes The transfer protocol of the API. Values MUST be from the list: "http",
 * "https", "ws", "wss". If the schemes is not included, the default scheme to be used is the one
 * used to access the Swagger definition itself.
 */
public enum Scheme {
  HTTP("http"),
  HTTPS("https"),
  WS("ws"),
  WSS("wss");

  private final String name;

  Scheme(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
