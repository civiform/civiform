package services.program.predicate;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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

  /** Get a leaf node if it exists, or throw if this is not a leaf node. */
  @JsonIgnore
  @Memoized
  public LeafOperationExpressionNode getLeafNode() {
    if (!(node() instanceof LeafOperationExpressionNode)) {
      throw new RuntimeException(
          String.format("Expected a LEAF node but received %s node", getType()));
    }
    return (LeafOperationExpressionNode) node();
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
  public ImmutableSet<Long> getQuestions() {
    switch (getType()) {
      case LEAF_OPERATION:
        return ImmutableSet.of(getLeafNode().questionId());
      case AND:
        return getAndNode().children().stream()
            .flatMap(n -> n.getQuestions().stream())
            .collect(toImmutableSet());
      case OR:
        return getOrNode().children().stream()
            .flatMap(n -> n.getQuestions().stream())
            .collect(toImmutableSet());
      default:
        return ImmutableSet.of();
    }
  }

  public String toDisplayString(ImmutableList<QuestionDefinition> questions) {
    return node().toDisplayString(questions);
  }
}
