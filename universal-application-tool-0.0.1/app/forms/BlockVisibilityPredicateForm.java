package forms;

import play.data.validation.Constraints;
import play.data.validation.Constraints.Validatable;
import services.applicant.question.Scalar;
import services.program.predicate.Operator;

public class BlockVisibilityPredicateForm implements Validatable<String> {
  @Constraints.Required(message = "Must select 'hidden if' or 'shown if'.")
  private String predicateAction;

  @Constraints.Required(message = "Question ID is required.")
  private long questionId;

  @Constraints.Required(message = "Scalar is required.")
  private String scalar;

  @Constraints.Required(message = "Operator is required.")
  private String operator;

  @Constraints.Required(message = "Value is required.")
  private String predicateValue;

  public BlockVisibilityPredicateForm(
      String predicateAction,
      long questionId,
      String scalar,
      String operator,
      String predicateValue) {
    this.predicateAction = predicateAction;
    this.questionId = questionId;
    this.scalar = scalar;
    this.operator = operator;
    this.predicateValue = predicateValue;
  }

  public BlockVisibilityPredicateForm() {
    predicateAction = "";
    // TODO(natsid): Default value for questionId? Should it be OptionalLong (see
    //  NumberQuestionForm)?
    scalar = "";
    operator = "";
    predicateValue = "";
  }

  @Override
  public String validate() {
    Operator operator = Operator.valueOf(getOperator());
    Scalar scalar = Scalar.valueOf(getScalar());

    // This should never happen since we only expose the usable operators for the given scalar.
    if (!operator.getOperableTypes().contains(scalar.toScalarType())) {
      return String.format(
          "Cannot use operator \"%s\" on scalar \"%s\".",
          operator.toDisplayString(), scalar.toDisplayString());
    }
    return null;
  }

  public String getPredicateAction() {
    return predicateAction;
  }

  public void setPredicateAction(String predicateAction) {
    this.predicateAction = predicateAction;
  }

  public long getQuestionId() {
    return questionId;
  }

  public void setQuestionId(long questionId) {
    this.questionId = questionId;
  }

  public String getScalar() {
    return scalar;
  }

  public void setScalar(String scalar) {
    this.scalar = scalar;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }

  public String getPredicateValue() {
    return predicateValue;
  }

  public void setPredicateValue(String predicateValue) {
    this.predicateValue = predicateValue;
  }
}
