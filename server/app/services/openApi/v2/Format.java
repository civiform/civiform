package services.openApi.v2;

/**
 * https://swagger.io/specification/v2/#data-type-format
 *
 * <p>See link for mapping between type and format
 */
public enum Format {
  INT32("int32"),
  INT64("int64"),
  FLOAT("float"),
  DOUBLE("double"),
  BYTE("byte"),
  BINARY("binary"),
  DATE("date"),
  DATETIME("date-time"),
  PASSWORD("password"),

  STRING("string"),
  ARRAY("array");

  private final String name;

  Format(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
