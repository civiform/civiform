package services.program.predicate;

/** Represents a JsonPath operator (https://github.com/json-path/JsonPath#filter-operators). */
public enum Operator {
  ANY_OF("anyof", "is any of"),
  EQUAL_TO("==", "is equal to"),
  GREATER_THAN(">", "is greater than"),
  GREATER_THAN_OR_EQUAL_TO(">=", "is greater than or equal to"),
  IN("in", "is in"),
  LESS_THAN("<", "is less than"),
  LESS_THAN_OR_EQUAL_TO("<=", "is less than or equal to"),
  NONE_OF("noneof", "is none of"),
  NOT_EQUAL_TO("!=", "is not equal to"),
  NOT_IN("nin", "is not in"),
  SUBSET_OF("subsetof", "is a subset of");

  private final String jsonPathOperator;
  private final String displayString;

  Operator(String jsonPathOperator, String displayString) {
    this.jsonPathOperator = jsonPathOperator;
    this.displayString = displayString;
  }

  public String toJsonPathOperator() {
    return this.jsonPathOperator;
  }

  public String toDisplayString() {
    return this.displayString;
  }
}
