package services.program.predicate;

import com.google.common.collect.ImmutableSet;
import services.question.types.ScalarType;

/** Represents a JsonPath operator (https://github.com/json-path/JsonPath#filter-operators). */
public enum Operator {
  ANY_OF("anyof", "contains any of", ImmutableSet.of(ScalarType.LIST_OF_STRINGS)),
  EQUAL_TO("==", "is equal to", ImmutableSet.of(ScalarType.LONG, ScalarType.STRING)),
  GREATER_THAN(">", "is greater than", ImmutableSet.of(ScalarType.LONG)),
  GREATER_THAN_OR_EQUAL_TO(">=", "is greater than or equal to", ImmutableSet.of(ScalarType.LONG)),
  IN("in", "is one of", ImmutableSet.of(ScalarType.STRING, ScalarType.LONG)),
  IS_AFTER(">=", "is later than", ImmutableSet.of(ScalarType.DATE)),
  IS_BEFORE("<=", "is earlier than", ImmutableSet.of(ScalarType.DATE)),
  LESS_THAN("<", "is less than", ImmutableSet.of(ScalarType.LONG)),
  LESS_THAN_OR_EQUAL_TO("<=", "is less than or equal to", ImmutableSet.of(ScalarType.LONG)),
  NONE_OF("noneof", "is none of", ImmutableSet.of(ScalarType.LIST_OF_STRINGS)),
  NOT_EQUAL_TO("!=", "is not equal to", ImmutableSet.of(ScalarType.LONG, ScalarType.STRING)),
  NOT_IN("nin", "is not one of", ImmutableSet.of(ScalarType.STRING, ScalarType.LONG)),
  SUBSET_OF("subsetof", "is a subset of", ImmutableSet.of(ScalarType.LIST_OF_STRINGS));

  private final String jsonPathOperator;
  private final String displayString;
  private final ImmutableSet<ScalarType> operableTypes;

  Operator(String jsonPathOperator, String displayString, ImmutableSet<ScalarType> operableTypes) {
    this.jsonPathOperator = jsonPathOperator;
    this.displayString = displayString;
    this.operableTypes = operableTypes;
  }

  public String toJsonPathOperator() {
    return this.jsonPathOperator;
  }

  public String toDisplayString() {
    return this.displayString;
  }

  /**
   * What type must the value on the left of the operator be? For example, "anyof" can only operate
   * on a list, since it compares "does X list contain any of the values in Y list?"
   */
  public ImmutableSet<ScalarType> getOperableTypes() {
    return this.operableTypes;
  }
}
