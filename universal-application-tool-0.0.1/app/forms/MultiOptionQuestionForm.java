package forms;

import com.google.common.collect.ImmutableListMultimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public abstract class MultiOptionQuestionForm extends QuestionForm {
  // TODO(https://github.com/seattle-uat/civiform/issues/354): Handle other locales besides
  // Locale.US
  // Caution: This must be a mutable list type, or else Play's form binding cannot add elements to
  // the list. This means the constructors MUST set this field to a mutable List type, NOT
  // ImmutableList.
  private List<String> options;
  private OptionalInt minChoicesRequired;
  private OptionalInt maxChoicesAllowed;

  protected MultiOptionQuestionForm(QuestionType type) {
    super();
    setQuestionType(type);
    this.options = new ArrayList<>();
    this.minChoicesRequired = OptionalInt.empty();
    this.maxChoicesAllowed = OptionalInt.empty();
  }

  protected MultiOptionQuestionForm(MultiOptionQuestionDefinition qd) {
    super(qd);
    setQuestionType(qd.getQuestionType());
    this.minChoicesRequired = qd.getMultiOptionValidationPredicates().minChoicesRequired();
    this.maxChoicesAllowed = qd.getMultiOptionValidationPredicates().maxChoicesAllowed();

    this.options = new ArrayList<>();
    if (qd.getOptions().containsKey(Locale.US)) {
      this.options.addAll(qd.getOptions().get(Locale.US));
    }
  }

  public List<String> getOptions() {
    return this.options;
  }

  public void setOptions(List<String> options) {
    this.options = options;
  }

  public OptionalInt getMinChoicesRequired() {
    return minChoicesRequired;
  }

  /**
   * We use a string parameter here so that if the field is empty (i.e. unset), we can correctly set
   * to an empty OptionalInt. Since the HTML input is type "number", we can be sure this string is
   * in fact an integer when we parse it. If we instead used an int here, we see an "Invalid value"
   * error when binding the empty value in the form.
   */
  public void setMinChoicesRequired(String minChoicesRequiredAsString) {
    if (minChoicesRequiredAsString.isEmpty()) {
      this.minChoicesRequired = OptionalInt.empty();
    } else {
      this.minChoicesRequired = OptionalInt.of(Integer.parseInt(minChoicesRequiredAsString));
    }
  }

  public OptionalInt getMaxChoicesAllowed() {
    return maxChoicesAllowed;
  }

  /**
   * We use a string parameter here so that if the field is empty (i.e. unset), we can correctly set
   * to an empty OptionalInt. Since the HTML input is type "number", we can be sure this string is
   * in fact an integer when we parse it. If we instead used an int here, we see an "Invalid value"
   * error when binding the empty value in the form.
   */
  public void setMaxChoicesAllowed(String maxChoicesAllowedAsString) {
    if (maxChoicesAllowedAsString.isEmpty()) {
      this.maxChoicesAllowed = OptionalInt.empty();
    } else {
      this.maxChoicesAllowed = OptionalInt.of(Integer.parseInt(maxChoicesAllowedAsString));
    }
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    MultiOptionQuestionDefinition.MultiOptionValidationPredicates.Builder predicateBuilder =
        MultiOptionQuestionDefinition.MultiOptionValidationPredicates.builder();

    if (getMinChoicesRequired().isPresent()) {
      predicateBuilder.setMinChoicesRequired(getMinChoicesRequired());
    }

    if (getMaxChoicesAllowed().isPresent()) {
      predicateBuilder.setMaxChoicesAllowed(getMaxChoicesAllowed());
    }

    return super.getBuilder()
        .setQuestionOptions(
            ImmutableListMultimap.<Locale, String>builder().putAll(Locale.US, getOptions()).build())
        .setValidationPredicates(predicateBuilder.build());
  }
}
