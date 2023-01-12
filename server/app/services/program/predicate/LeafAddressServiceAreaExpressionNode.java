package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.question.types.QuestionDefinition;

/** Represents an assertion that an address question is within a given service area. */
@JsonTypeName("leafAddressServiceArea")
@AutoValue
public abstract class LeafAddressServiceAreaExpressionNode
    implements ConcretePredicateExpressionNode {

  @JsonCreator
  public static LeafAddressServiceAreaExpressionNode create(
      @JsonProperty("questionId") long questionId,
      @JsonProperty("serviceAreaLabel") String serviceAreaLabel) {
    return builder().setQuestionId(questionId).setServiceAreaLabel(serviceAreaLabel).build();
  }

  /** The ID of the address {@link services.question.types.QuestionDefinition} this node checks. */
  @JsonProperty("questionId")
  public abstract long questionId();

  /** The string label of the service area the address should be checked for inclusion in. */
  @JsonProperty("serviceAreaLabel")
  public abstract String serviceAreaLabel();

  @Override
  @JsonIgnore
  public PredicateExpressionNodeType getType() {
    return PredicateExpressionNodeType.LEAF_ADDRESS_SERVICE_AREA;
  }

  @Override
  @JsonIgnore
  public void accept(PredicateExpressionNodeVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Displays a human-readable representation of the assertion in the format "[question name] is in
   * service area [service area label]".
   */
  @Override
  public String toDisplayString(ImmutableList<QuestionDefinition> questions) {
    Optional<QuestionDefinition> maybeQuestionDefinition =
        questions.stream().filter(q -> q.getId() == questionId()).findFirst();

    String addressLabel =
        maybeQuestionDefinition
            .map(QuestionDefinition::getName)
            .map(addressName -> String.format("\"%s\"", addressName))
            .orElse("address");

    return String.format("%s is in service area \"%s\"", addressLabel, serviceAreaLabel());
  }

  public static Builder builder() {
    return new AutoValue_LeafAddressServiceAreaExpressionNode.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setQuestionId(long questionId);

    public abstract Builder setServiceAreaLabel(String serviceAreaLabel);

    public abstract LeafAddressServiceAreaExpressionNode build();
  }
}
