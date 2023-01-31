package services.program.predicate;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import services.question.types.ScalarType;

/** Represents a JsonPath operator (https://github.com/json-path/JsonPath#filter-operators). */
public enum Operator {
  ANY_OF(
      "anyof",
      "contains any of",
      ImmutableSet.of(ScalarType.LIST_OF_STRINGS),
      ImmutableSet.of(OperatorRightHandType.LIST_OF_LONGS, OperatorRightHandType.LIST_OF_STRINGS)),
  EQUAL_TO(
      "==",
      "is equal to",
      ImmutableSet.of(ScalarType.CURRENCY_CENTS, ScalarType.LONG, ScalarType.STRING),
      ImmutableSet.of(
          OperatorRightHandType.DATE, OperatorRightHandType.STRING, OperatorRightHandType.LONG)),
  GREATER_THAN(
      ">",
      "is greater than",
      ImmutableSet.of(ScalarType.CURRENCY_CENTS, ScalarType.LONG),
      ImmutableSet.of(OperatorRightHandType.LONG)),
  GREATER_THAN_OR_EQUAL_TO(
      ">=",
      "is greater than or equal to",
      ImmutableSet.of(ScalarType.CURRENCY_CENTS, ScalarType.LONG),
      ImmutableSet.of(OperatorRightHandType.LONG)),
  IN(
      "in",
      "is one of",
      ImmutableSet.of(ScalarType.STRING, ScalarType.LONG),
      ImmutableSet.of(OperatorRightHandType.LIST_OF_LONGS, OperatorRightHandType.LIST_OF_STRINGS)),
  IS_AFTER(
      ">",
      "is later than",
      ImmutableSet.of(ScalarType.DATE),
      ImmutableSet.of(OperatorRightHandType.DATE)),
  IS_ON_OR_AFTER(
      ">=",
      "is on or later than",
      ImmutableSet.of(ScalarType.DATE),
      ImmutableSet.of(OperatorRightHandType.DATE)),
  IS_BEFORE(
      "<",
      "is earlier than",
      ImmutableSet.of(ScalarType.DATE),
      ImmutableSet.of(OperatorRightHandType.DATE)),
  IS_ON_OR_BEFORE(
      "<=",
      "is on or earlier than",
      ImmutableSet.of(ScalarType.DATE),
      ImmutableSet.of(OperatorRightHandType.DATE)),
  LESS_THAN(
      "<",
      "is less than",
      ImmutableSet.of(ScalarType.CURRENCY_CENTS, ScalarType.LONG),
      ImmutableSet.of(OperatorRightHandType.LONG)),
  LESS_THAN_OR_EQUAL_TO(
      "<=",
      "is less than or equal to",
      ImmutableSet.of(ScalarType.CURRENCY_CENTS, ScalarType.LONG),
      ImmutableSet.of(OperatorRightHandType.LONG)),
  NONE_OF(
      "noneof",
      "is none of",
      ImmutableSet.of(ScalarType.LIST_OF_STRINGS),
      ImmutableSet.of(OperatorRightHandType.LIST_OF_LONGS, OperatorRightHandType.LIST_OF_STRINGS)),
  NOT_EQUAL_TO(
      "!=",
      "is not equal to",
      ImmutableSet.of(ScalarType.CURRENCY_CENTS, ScalarType.LONG, ScalarType.STRING),
      ImmutableSet.of(
          OperatorRightHandType.DATE, OperatorRightHandType.STRING, OperatorRightHandType.LONG)),
  NOT_IN(
      "nin",
      "is not one of",
      ImmutableSet.of(ScalarType.STRING, ScalarType.LONG),
      ImmutableSet.of(OperatorRightHandType.LIST_OF_LONGS, OperatorRightHandType.LIST_OF_STRINGS)),
  SUBSET_OF(
      "subsetof",
      "is a subset of",
      ImmutableSet.of(ScalarType.LIST_OF_STRINGS),
      ImmutableSet.of(OperatorRightHandType.LIST_OF_LONGS, OperatorRightHandType.LIST_OF_STRINGS)),
  IN_SERVICE_AREA(
      "n/a",
      "in service area",
      ImmutableSet.of(ScalarType.SERVICE_AREA),
      ImmutableSet.of(OperatorRightHandType.SERVICE_AREA));

  private final String jsonPathOperator;
  private final String displayString;
  private final ImmutableSet<ScalarType> operableTypes;
  private final ImmutableSet<OperatorRightHandType> rightHandTypes;

  Operator(
      String jsonPathOperator,
      String displayString,
      ImmutableSet<ScalarType> operableTypes,
      ImmutableSet<OperatorRightHandType> rightHandTypes) {
    this.jsonPathOperator = checkNotNull(jsonPathOperator);
    this.displayString = checkNotNull(displayString);
    this.operableTypes = checkNotNull(operableTypes);
    this.rightHandTypes = checkNotNull(rightHandTypes);
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

  /** The types allowed for the right hand (i.e. comparison value) of this operator. */
  public ImmutableSet<OperatorRightHandType> getRightHandTypes() {
    return this.rightHandTypes;
  }
}
