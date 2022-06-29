package forms;

import java.util.ArrayList;
import java.util.List;
import play.data.validation.Constraints;
import play.data.validation.Constraints.Validatable;
import play.data.validation.Constraints.Validate;
import play.data.validation.ValidationError;
import services.applicant.question.Scalar;
import services.program.predicate.Operator;

/** Form for updating visibility configurations of a screen (block) of a program. */
@Validate
public class BlockVisibilityPredicateForm implements Validatable<List<ValidationError>> {
  @Constraints.Required(message = "Must select 'hidden if' or 'shown if'.")
  private String predicateAction;

  @Constraints.Required(message = "Question ID is required.")
  private long questionId;

  @Constraints.Required(message = "Scalar is required.")
  private String scalar;

  @Constraints.Required(message = "Operator is required.")
  private String operator;

  /**
   * Either predicateValue OR predicateValues must be present. But because this validation logic is
   * more complex than can be described by {@link Constraints.Required}, we add it to the validate
   * method below.
   */
  private String predicateValue;

  /**
   * This value is used when the question ID is for a multi-option question.
   *
   * <p>Caution: This must be a mutable list type, or else Play's form binding cannot add elements
   * to the list. This means the constructors MUST set this field to a mutable List type, NOT
   * ImmutableList.
   */
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
  public List<ValidationError> validate() {
    ArrayList<ValidationError> errors = new ArrayList<>();

    // The rest of this validate method depends on scalar being present.
    if (scalar.isEmpty()) return errors;

    Scalar scalarEnum = Scalar.valueOf(getScalar());

    // Only attempt to run the scalar-operator compatibility validation if both values are present.
    if (!operator.isEmpty()) {
      Operator operatorEnum = Operator.valueOf(getOperator());

      // This should never happen since we only expose the usable operators for the given scalar.
      if (!operatorEnum.getOperableTypes().contains(scalarEnum.toScalarType())) {
        errors.add(
            new ValidationError(
                "operator",
                String.format(
                    "Cannot use operator \"%s\" on scalar \"%s\".",
                    operatorEnum.toDisplayString(), scalarEnum.toDisplayString())));
      }
    }

    if ((scalarEnum == Scalar.SELECTION || scalarEnum == Scalar.SELECTIONS)) {
      if (predicateValues.isEmpty()) {
        errors.add(new ValidationError("predicateValues", "Must select at least one value."));
      }
    } else if (predicateValue.isEmpty()) {
      errors.add(new ValidationError("predicateValue", "Value is required."));
    }

    return errors;
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
