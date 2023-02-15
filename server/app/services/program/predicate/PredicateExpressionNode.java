package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import services.question.types.QuestionDefinition;

/** Represents a predicate that can be evaluated over {@link services.applicant.ApplicantData}. */
@AutoValue
public abstract class PredicateExpressionNode {

  @JsonCreator
  public static PredicateExpressionNode create(
      @JsonProperty("node") ConcretePredicateExpressionNode node) {
    return new AutoValue_PredicateExpressionNode(node);
  }

  @JsonProperty("node")
  public abstract ConcretePredicateExpressionNode node();

  @JsonIgnore
  @Memoized
  public PredicateExpressionNodeType getType() {
    return node().getType();
  }

  @JsonIgnore
  public void accept(PredicateExpressionNodeVisitor predicateExpressionNodeVisitor) {
    node().accept(predicateExpressionNodeVisitor);
  }

  /** Get a leaf operation node if it exists, or throw if this is not a leaf operation node. */
  @JsonIgnore
  @Memoized
  public LeafOperationExpressionNode getLeafOperationNode() {
    if (!(node() instanceof LeafOperationExpressionNode)) {
      throw new RuntimeException(
          String.format("Expected a LEAF node but received %s node", getType()));
    }
    return (LeafOperationExpressionNode) node();
  }

  /** Get a leaf node if it exists, or throw if this is not a leaf node. */
  @JsonIgnore
  @Memoized
  public LeafExpressionNode getLeafNode() {
    if (!(node() instanceof LeafExpressionNode)) {
      throw new RuntimeException(
          String.format(
              "Expected a LEAF or LEAF_ADDRESS_SERVICE_AREA node but received %s node", getType()));
    }
    return (LeafExpressionNode) node();
  }

  /** Get a leaf address node if it exists, or throw if this is not a leaf address node. */
  @JsonIgnore
  @Memoized
  public LeafAddressServiceAreaExpressionNode getLeafAddressNode() {
    if (!(node() instanceof LeafAddressServiceAreaExpressionNode)) {
      throw new RuntimeException(
          String.format(
              "Expected a LEAF_ADDRESS_SERVICE_AREA node but received %s node", getType()));
    }
    return (LeafAddressServiceAreaExpressionNode) node();
  }

  /** Get an and node if it exists, or throw if this is not an and node. */
  @JsonIgnore
  @Memoized
  public AndNode getAndNode() {
    if (!(node() instanceof AndNode)) {
      throw new RuntimeException(
          String.format("Expected an AND node but received %s node", getType()));
    }
    return (AndNode) node();
  }

  /** Get an or node if it exists, or throw if this is not an or node. */
  @JsonIgnore
  @Memoized
  public OrNode getOrNode() {
    if (!(node() instanceof OrNode)) {
      throw new RuntimeException(
          String.format("Expected an OR node but received %s node", getType()));
    }
    return (OrNode) node();
  }

  @JsonIgnore
  @Memoized
  public ImmutableList<Long> getQuestions() {
    switch (getType()) {
      case LEAF_ADDRESS_SERVICE_AREA:
        return ImmutableList.of(getLeafAddressNode().questionId());
      case LEAF_OPERATION:
        return ImmutableList.of(getLeafOperationNode().questionId());
      case AND:
        return getAndNode().children().stream()
            .flatMap(n -> n.getQuestions().stream())
            .collect(ImmutableList.toImmutableList());
      case OR:
        return getOrNode().children().stream()
            .flatMap(n -> n.getQuestions().stream())
            .collect(ImmutableList.toImmutableList());
      default:
        return ImmutableList.of();
    }
  }

  public String toDisplayString(ImmutableList<QuestionDefinition> questions) {
    return node().toDisplayString(questions);
  }
}
