package services.program.predicate;

/**
 * An enum defining the types of nodes supported in a predicate expression. A predicate expression
 * is represented as a tree structure. A terminal node can be evaluated by itself while a
 * non-terminal node is evaluated based on the results of its children.
 */
public enum PredicateExpressionNodeType {
  AND("all"),
  OR("any"),
  LEAF_OPERATION(""),
  LEAF_ADDRESS_SERVICE_AREA("");

  private final String displayString;

  PredicateExpressionNodeType(String displayString) {
    this.displayString = displayString;
  }

  public String toDisplayString() {
    return displayString;
  }

  public Boolean isLeafNode() {
    return this == LEAF_OPERATION || this == LEAF_ADDRESS_SERVICE_AREA;
  }
}
