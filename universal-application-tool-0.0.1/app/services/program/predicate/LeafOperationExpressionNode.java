package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import services.applicant.question.Scalar;

/**
 * Represents a JsonPath (https://github.com/json-path/JsonPath) expression for a single scalar in
 * the format: {@code [json_key][operator][value]} The expression must be in the context of a single
 * question.
 */
@JsonTypeName("leaf")
@AutoValue
public abstract class LeafOperationExpressionNode implements ConcretePredicateExpressionNode {

  @JsonCreator
  public static LeafOperationExpressionNode create(
      @JsonProperty("questionId") long questionId,
      @JsonProperty("scalar") Scalar scalar,
      @JsonProperty("operator") Operator operator,
      @JsonProperty("value") PredicateValue comparedValue) {
    return builder()
        .questionId(questionId)
        .scalar(scalar)
        .operator(operator)
        .comparedValue(comparedValue)
        .build();
  }

  /**
   * The ID of the {@link services.question.types.QuestionDefinition} this expression operates on.
   */
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

  public static Builder builder() {
    return new AutoValue_LeafOperationExpressionNode.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder questionId(long questionId);

    public abstract Builder scalar(Scalar scalar);

    public abstract Builder operator(Operator operator);

    public abstract Builder comparedValue(PredicateValue comparedValue);

    public abstract LeafOperationExpressionNode build();
  }
}
