package services.program.predicate;

/**
 * An enum defining the types supported on the "right hand side" of an {@link Operator} in a {@link
 * PredicateExpressionNode}, where the order of the expression looks like {@link
 * services.question.types.ScalarType} {@link Operator} {@link OperatorRightHandType}.
 *
 * <ul>
 *   For example:
 *   <li>STRING IS_EQUAL_TO STRING
 *   <li>DATE IS_AFTER DATE
 *   <li>LONG NOT_IN LIST_OF_LONGS
 * </ul>
 */
public enum OperatorRightHandType {
  DATE,
  LIST_OF_LONGS,
  LIST_OF_STRINGS,
  LONG,
  STRING,
  SERVICE_AREA
}
