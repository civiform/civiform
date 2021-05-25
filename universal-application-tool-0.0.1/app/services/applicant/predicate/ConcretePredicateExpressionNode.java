package services.applicant.predicate;

public interface ConcretePredicateExpressionNode {

  /**
   * Returns the type of this node, as a {@link
   * services.applicant.predicate.PredicateExpressionNodeType}.
   */
  PredicateExpressionNodeType getType();
}
