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

  public void setMinChoicesRequired(int minChoicesRequired) {
    this.minChoicesRequired = OptionalInt.of(minChoicesRequired);
  }

  public OptionalInt getMaxChoicesAllowed() {
    return maxChoicesAllowed;
  }

  public void setMaxChoicesAllowed(int maxChoicesAllowed) {
    this.maxChoicesAllowed = OptionalInt.of(maxChoicesAllowed);
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
