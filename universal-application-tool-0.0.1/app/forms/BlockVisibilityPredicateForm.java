package forms;

import java.util.ArrayList;
import java.util.List;
import play.data.validation.Constraints;
import play.data.validation.Constraints.Validatable;
import play.data.validation.Constraints.Validate;
import services.applicant.question.Scalar;
import services.program.predicate.Operator;

@Validate
public class BlockVisibilityPredicateForm implements Validatable<String> {
  @Constraints.Required(message = "Must select 'hidden if' or 'shown if'.")
  private String predicateAction;

  @Constraints.Required(message = "Question ID is required.")
  private long questionId;

  @Constraints.Required(message = "Scalar is required.")
  private String scalar;

  @Constraints.Required(message = "Operator is required.")
  private String operator;

  // TODO(natsid): probably need to add this to our custom validate method
  //  since it's not as straightforward "required" anymore.
  //  Specifically: need ONE OF value or values.
  @Constraints.Required(message = "Value is required.")
  private String predicateValue;

  // This value is used when the question ID is for a multi-option question.
  // Caution: This must be a mutable list type, or else Play's form binding cannot add elements to
  // the list. This means the constructors MUST set this field to a mutable List type, NOT
  // ImmutableList.
  private List<String> predicateValues;

  public BlockVisibilityPredicateForm(
      String predicateAction,
      long questionId,
      String scalar,
      String operator,
      String predicateValue,
      List<String> predicateValues) {
    this.predicateAction = predicateAction;
    this.questionId = questionId;
    this.scalar = scalar;
    this.operator = operator;
    this.predicateValue = predicateValue;
    this.predicateValues = predicateValues;
  }

  public BlockVisibilityPredicateForm() {
    predicateAction = "";
    // TODO(natsid): Default value for questionId? Should it be OptionalLong (see
    //  NumberQuestionForm)?
    scalar = "";
    operator = "";
    predicateValue = "";
    predicateValues = new ArrayList<>();
  }

  @Override
  public String validate() {
    // Don't attempt to run this validate method if missing required values.
    if (operator.isEmpty() || scalar.isEmpty()) return null;

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

  public List<String> getPredicateValues() {
    return predicateValues;
  }

  public void setPredicateValues(List<String> predicateValues) {
    this.predicateValues = predicateValues;
  }
}
