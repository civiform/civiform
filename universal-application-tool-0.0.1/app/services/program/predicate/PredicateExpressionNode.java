package services.program.predicate;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Represents a predicate that can be evaluated over {@link services.applicant.ApplicantData}. */
public class PredicateExpressionNode {

  @JsonProperty("node")
  private ConcretePredicateExpressionNode node;

  @JsonCreator
  public PredicateExpressionNode(@JsonProperty("node") ConcretePredicateExpressionNode node) {
    this.node = checkNotNull(node);
  }

  @JsonIgnore
  public PredicateExpressionNodeType getType() {
    return node.getType();
  }

  /** Get a leaf node if it exists, or throw if this is not a leaf node. */
  @JsonIgnore
  public LeafOperationExpressionNode getLeafNode() {
    if (!(node instanceof LeafOperationExpressionNode)) {
      throw new RuntimeException(
          String.format("Expected a leaf node but received %s node", getType()));
    }
    return (LeafOperationExpressionNode) node;
  }
}
