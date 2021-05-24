package services.applicant.predicate;

import javax.annotation.Nullable;

/** Represents a predicate that can be evaluated over {@link services.applicant.ApplicantData}. */
public class PredicateExpressionNode {

  private final PredicateExpressionNodeType type;
  private final LeafOperationExpressionNode leafNode;

  public PredicateExpressionNode(
      PredicateExpressionNodeType type, @Nullable LeafOperationExpressionNode leafNode) {
    this.type = type;
    this.leafNode = leafNode;
  }

  public PredicateExpressionNodeType getType() {
    return this.type;
  }

  /** Get a leaf node if it exists, or throw if this is not a leaf node. */
  public LeafOperationExpressionNode getLeafNode() {
    if (leafNode == null) {
      throw new RuntimeException("Tried to get a predicate leaf node but no node exists");
    }
    return this.leafNode;
  }
}
