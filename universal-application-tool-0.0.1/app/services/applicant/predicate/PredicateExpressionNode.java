package services.applicant.predicate;

import javax.annotation.Nullable;

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

  public LeafOperationExpressionNode getLeafNode() {
    if (leafNode == null) {
      throw new RuntimeException("Tried to get a leaf node but no node exists");
    }
    return this.leafNode;
  }
}
