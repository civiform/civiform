package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.regex.Pattern;
import services.question.types.QuestionDefinition;

/** Represents an assertion that an address question is within a given service area. */
@JsonTypeName("leafAddressServiceArea")
@AutoValue
public abstract class LeafAddressServiceAreaExpressionNode implements LeafExpressionNode {
  public static final Pattern SERVICE_AREA_ID_PATTERN = Pattern.compile("[a-zA-Z\\d\\-]+");

  @JsonCreator
  public static LeafAddressServiceAreaExpressionNode create(
      @JsonProperty("questionId") long questionId,
      @JsonProperty("serviceAreaId") String serviceAreaId) {
    return builder().setQuestionId(questionId).setServiceAreaId(serviceAreaId).build();
  }

  /** The ID of the address {@link services.question.types.QuestionDefinition} this node checks. */
  @Override
  @JsonProperty("questionId")
  public abstract long questionId();

  /** The string ID of the service area the address should be checked for inclusion in. */
  @JsonProperty("serviceAreaId")
  public abstract String serviceAreaId();

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
   * service area [service area ID]".
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

    return String.format("%s is in service area \"%s\"", addressLabel, serviceAreaId());
  }

  public static Builder builder() {
    return new AutoValue_LeafAddressServiceAreaExpressionNode.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setQuestionId(long questionId);

    public abstract Builder setServiceAreaId(String serviceAreaId);

    public abstract LeafAddressServiceAreaExpressionNode build();
  }
}
