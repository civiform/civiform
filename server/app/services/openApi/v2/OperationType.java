package services.openApi.v2;

/**
 * The allowed list of operations, a.k.a. Http Verbs.
 *
 * <p>https://swagger.io/specification/v2/#path-item-object
 */
public enum OperationType {
  GET("get"),
  PUT("put"),
  POST("post"),
  DELETE("delete"),
  OPTIONS("options"),
  HEAD("head"),
  PATCH("patch");

  private final String name;

  OperationType(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
