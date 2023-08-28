package services.openApi.v2;

/**
 * https://swagger.io/specification/v2/#data-type-format
 *
 * <p>See link for mapping between type and format.
 */
public enum DefinitionType {
  OBJECT("object"),
  ARRAY("array"),

  STRING("string"),
  INTEGER("integer"),
  NUMBER("number"),
  BOOLEAN("boolean");

  private final String name;

  DefinitionType(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
