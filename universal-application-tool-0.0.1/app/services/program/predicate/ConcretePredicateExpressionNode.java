package services.program.predicate;

public interface ConcretePredicateExpressionNode {

  /** Returns the type of this node, as a {@link PredicateExpressionNodeType}. */
  PredicateExpressionNodeType getType();
}
