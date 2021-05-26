package services.program.predicate;

import com.google.auto.value.AutoValue;
import services.applicant.question.Scalar;

/**
 * Represents a JsonPath (https://github.com/json-path/JsonPath) expression for a single scalar in
 * the format: {@code [json_key][operator][value]} The expression must be in the context of a single
 * question.
 */
@AutoValue
public abstract class LeafOperationExpressionNode implements ConcretePredicateExpressionNode {

  public static LeafOperationExpressionNode create(
      long questionId, Scalar scalar, Operator operator, PredicateValue comparedValue) {
    return new AutoValue_LeafOperationExpressionNode(questionId, scalar, operator, comparedValue);
  }

  /**
   * The ID of the {@link services.question.types.QuestionDefinition} this expression operates on.
   */
  public abstract long questionId();

  /** The specific {@link Scalar} to operate on. */
  public abstract Scalar scalar();

  /** The operator for this expression. */
  public abstract Operator operator();

  /** The value to compare the question key to using the operator, represented as a string. */
  public abstract PredicateValue comparedValue();

  @Override
  public PredicateExpressionNodeType getType() {
    return PredicateExpressionNodeType.LEAF_OPERATION;
  }
}
