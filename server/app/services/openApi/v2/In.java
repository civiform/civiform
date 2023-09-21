package services.openApi.v2;

/**
 * The location of the parameter. Possible values are "query", "header", "path", "formData" or
 * "body".
 *
 * <p>https://swagger.io/specification/v2/#parameter-object
 */
public enum In {
  QUERY("query"),
  HEADER("header"),
  PATH("path"),
  FORMDATA("formData")
// Not supporting BODY right now since it operates much differently than the other options and isn't
// something we'll use right now
//  BODY("body")
;

  private final String name;

  In(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
