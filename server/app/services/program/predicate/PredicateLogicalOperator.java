package services.program.predicate;

/**
 * The supported use cases for predicate logic.
 *
 * <p>A subset of {@link PredicateExpressionNodeType} that only covers logical operators.
 */
public enum PredicateLogicalOperator {
  AND(PredicateExpressionNodeType.AND, "and"),
  OR(PredicateExpressionNodeType.OR, "or");

  private final PredicateExpressionNodeType nodeType;
  private final String displayString;

  PredicateLogicalOperator(PredicateExpressionNodeType nodeType, String displayString) {
    this.nodeType = nodeType;
    this.displayString = displayString;
  }

  public String toDisplayString() {
    return displayString;
  }

  public PredicateExpressionNodeType toNodeType() {
    return nodeType;
  }
}
