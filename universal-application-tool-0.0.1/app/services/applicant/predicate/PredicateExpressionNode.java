package services.applicant.predicate;

import static com.google.common.base.Preconditions.checkNotNull;

/** Represents a predicate that can be evaluated over {@link services.applicant.ApplicantData}. */
public class PredicateExpressionNode {

  private final ConcretePredicateExpressionNode node;

  public PredicateExpressionNode(ConcretePredicateExpressionNode node) {
    this.node = checkNotNull(node);
  }

  public PredicateExpressionNodeType getType() {
    return node.getType();
  }

  /** Get a leaf node if it exists, or throw if this is not a leaf node. */
  public LeafOperationExpressionNode getLeafNode() {
    if (!(node instanceof LeafOperationExpressionNode)) {
      throw new RuntimeException(
          String.format("Expected a leaf node but received %s node", getType()));
    }
    return (LeafOperationExpressionNode) node;
  }
}
