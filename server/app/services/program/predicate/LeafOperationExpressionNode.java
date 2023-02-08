package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.applicant.question.Scalar;
import services.question.types.QuestionDefinition;

/**
 * Represents a JsonPath (https://github.com/json-path/JsonPath) expression for a single scalar in
 * the format: {@code [json_key][operator][value]} The expression must be in the context of a single
 * question.
 */
@JsonTypeName("leaf")
@AutoValue
public abstract class LeafOperationExpressionNode implements LeafExpressionNode {

  @JsonCreator
  public static LeafOperationExpressionNode create(
      @JsonProperty("questionId") long questionId,
      @JsonProperty("scalar") Scalar scalar,
      @JsonProperty("operator") Operator operator,
      @JsonProperty("value") PredicateValue comparedValue) {
    return builder()
        .setQuestionId(questionId)
        .setScalar(scalar)
        .setOperator(operator)
        .setComparedValue(comparedValue)
        .build();
  }

  /**
   * The ID of the {@link services.question.types.QuestionDefinition} this expression operates on.
   */
  @Override
  @JsonProperty("questionId")
  public abstract long questionId();

  /** The specific {@link Scalar} to operate on. */
  @JsonProperty("scalar")
  public abstract Scalar scalar();

  /** The operator for this expression. */
  @JsonProperty("operator")
  public abstract Operator operator();

  /** The value to compare the question key to using the operator, represented as a string. */
  @JsonProperty("value")
  public abstract PredicateValue comparedValue();

  @Override
  @JsonIgnore
  public PredicateExpressionNodeType getType() {
    return PredicateExpressionNodeType.LEAF_OPERATION;
  }

  @Override
  @JsonIgnore
  public void accept(PredicateExpressionNodeVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Displays a human-readable representation of this expression, in the format "[question name]'s
   * [scalar] [operator] [value]". For example: "home address's city is one of ["Seattle",
   * "Tacoma"]"
   */
  @Override
  public String toDisplayString(ImmutableList<QuestionDefinition> questions) {
    Optional<QuestionDefinition> question =
        questions.stream().filter(q -> q.getId() == questionId()).findFirst();
    String displayValue =
        question
            .map(q -> comparedValue().toDisplayString(q))
            .orElseGet(() -> comparedValue().value());
    String phrase =
        Joiner.on(' ').join(scalar().toDisplayString(), operator().toDisplayString(), displayValue);
    return question.isEmpty()
        ? phrase
        : String.format("\"%s\" %s", question.get().getName(), phrase);
  }

  public static Builder builder() {
    return new AutoValue_LeafOperationExpressionNode.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setQuestionId(long questionId);

    public abstract Builder setScalar(Scalar scalar);

    public abstract Builder setOperator(Operator operator);

    public abstract Builder setComparedValue(PredicateValue comparedValue);

    public abstract LeafOperationExpressionNode build();
  }
}
