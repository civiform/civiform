package forms;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import services.Path;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;
import services.question.exceptions.TranslationNotFoundException;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

public abstract class MultiOptionQuestionForm extends QuestionForm {
  // TODO(https://github.com/seattle-uat/civiform/issues/354): Handle other locales besides
  //  Locale.US
  // Caution: This must be a mutable list type, or else Play's form binding cannot add elements to
  // the list. This means the constructors MUST set this field to a mutable List type, NOT
  // ImmutableList.
  private List<String> options;
  private OptionalInt minChoicesRequired;
  private OptionalInt maxChoicesAllowed;

  protected MultiOptionQuestionForm() {
    super();
    this.options = new ArrayList<>();
    this.minChoicesRequired = OptionalInt.empty();
    this.maxChoicesAllowed = OptionalInt.empty();
  }

  protected MultiOptionQuestionForm(MultiOptionQuestionDefinition qd) {
    super(qd);
    this.minChoicesRequired = qd.getMultiOptionValidationPredicates().minChoicesRequired();
    this.maxChoicesAllowed = qd.getMultiOptionValidationPredicates().maxChoicesAllowed();

    this.options = new ArrayList<>();

    try {
      // TODO: this will need revisiting to support multiple locales
      if (qd.getSupportedLocales().contains(Locale.US)) {
        List<String> optionStrings =
            qd.getOptionsForLocale(Locale.US).stream()
                .map(LocalizedQuestionOption::optionText)
                .collect(Collectors.toList());
        this.options.addAll(optionStrings);
      }
    } catch (TranslationNotFoundException e) {
      throw new RuntimeException(e);
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
  public QuestionDefinitionBuilder getBuilder(Path path) {
    MultiOptionQuestionDefinition.MultiOptionValidationPredicates.Builder predicateBuilder =
        MultiOptionQuestionDefinition.MultiOptionValidationPredicates.builder();

    if (getMinChoicesRequired().isPresent()) {
      predicateBuilder.setMinChoicesRequired(getMinChoicesRequired());
    }

    if (getMaxChoicesAllowed().isPresent()) {
      predicateBuilder.setMaxChoicesAllowed(getMaxChoicesAllowed());
    }

    // TODO: this will need to be revisited to support multiple locales
    ImmutableList.Builder<QuestionOption> questionOptions = ImmutableList.builder();
    long optionCount = 1L;

    for (String optionText : getOptions()) {
      questionOptions.add(
          QuestionOption.create(optionCount++, ImmutableMap.of(Locale.US, optionText)));
    }

    return super.getBuilder(path)
        .setQuestionOptions(questionOptions.build())
        .setValidationPredicates(predicateBuilder.build());
  }
}
