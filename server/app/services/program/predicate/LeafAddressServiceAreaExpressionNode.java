package services.program.predicate;

import static j2html.TagCreator.join;
import static j2html.TagCreator.strong;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import j2html.tags.UnescapedText;
import java.util.Optional;
import services.question.types.QuestionDefinition;

/** Represents an assertion that an address question is within a given service area. */
@JsonTypeName("leafAddressServiceArea")
@AutoValue
public abstract class LeafAddressServiceAreaExpressionNode implements LeafExpressionNode {
  @JsonCreator
  public static LeafAddressServiceAreaExpressionNode create(
      @JsonProperty("questionId") long questionId,
      @JsonProperty("serviceAreaId") String serviceAreaId,
      @JsonProperty("operator") Operator operator) {
    return builder()
        .setQuestionId(questionId)
        .setServiceAreaId(serviceAreaId)
        .setOperator(operator)
        .build();
  }

  /** The ID of the address {@link services.question.types.QuestionDefinition} this node checks. */
  @Override
  @JsonProperty("questionId")
  public abstract long questionId();

  /** The string ID of the service area the address should be checked for inclusion in. */
  @JsonProperty("serviceAreaId")
  public abstract String serviceAreaId();

  /** The operator for this expression. */
  @JsonProperty("operator")
  public abstract Operator operator();

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
    return String.format(
        "%s is %s \"%s\"",
        getAddressLabel(questions), operator().toDisplayString(), serviceAreaId());
  }

  /**
   * Displays a formatted, human-readable representation of the assertion in HTML, in the format
   * "<strong>[question name]</strong> is in service area <strong>[service area ID]</strong>".
   */
  @Override
  public UnescapedText toDisplayFormattedHtml(ImmutableList<QuestionDefinition> questions) {
    return join(
        strong(getAddressLabel(questions)),
        "is",
        operator().toDisplayString(),
        strong(String.format("\"%s\"", serviceAreaId())));
  }

  String getAddressLabel(ImmutableList<QuestionDefinition> questions) {
    Optional<QuestionDefinition> maybeQuestionDefinition =
        questions.stream().filter(q -> q.getId() == questionId()).findFirst();

    return maybeQuestionDefinition
        .map(QuestionDefinition::getName)
        .map(addressName -> String.format("\"%s\"", addressName))
        .orElse("address");
  }

  public static Builder builder() {
    return new AutoValue_LeafAddressServiceAreaExpressionNode.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setQuestionId(long questionId);

    public abstract Builder setServiceAreaId(String serviceAreaId);

    public abstract Builder setOperator(Operator operator);

    public abstract LeafAddressServiceAreaExpressionNode build();
  }
}
