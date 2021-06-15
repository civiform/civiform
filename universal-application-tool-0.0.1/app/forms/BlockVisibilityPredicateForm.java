package forms;

import play.data.validation.Constraints;

public class BlockVisibilityPredicateForm {
  @Constraints.Required private String predicateAction;
  @Constraints.Required private long questionId;
  @Constraints.Required private String scalar;
  @Constraints.Required private String operator;
  @Constraints.Required private String predicateValue;

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
    this.predicateValue = "";
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
