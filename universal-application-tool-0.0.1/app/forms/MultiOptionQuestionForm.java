package forms;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

/** Superclass for all forms for updating a multi-option question. */
public abstract class MultiOptionQuestionForm extends QuestionForm {
  // Caution: This must be a mutable list type, or else Play's form binding cannot add elements to
  // the list. This means the constructors MUST set this field to a mutable List type, NOT
  // ImmutableList.
  private List<String> options;
  // Options added to the list during the edit.
  private List<String> newOptions;
  private List<Long> optionIds;
  private OptionalLong nextAvailableId;
  private OptionalInt minChoicesRequired;
  private OptionalInt maxChoicesAllowed;

  protected MultiOptionQuestionForm() {
    super();
    this.options = new ArrayList<>();
    this.newOptions = new ArrayList<>();
    this.optionIds = new ArrayList<>();
    this.minChoicesRequired = OptionalInt.empty();
    this.maxChoicesAllowed = OptionalInt.empty();
    this.nextAvailableId = OptionalLong.empty();
  }

  protected MultiOptionQuestionForm(MultiOptionQuestionDefinition qd) {
    super(qd);
    this.minChoicesRequired = qd.getMultiOptionValidationPredicates().minChoicesRequired();
    this.maxChoicesAllowed = qd.getMultiOptionValidationPredicates().maxChoicesAllowed();

    this.options = new ArrayList<>();
    this.newOptions = new ArrayList<>();
    this.optionIds = new ArrayList<>();

    try {
      // The first time a question is created, we only create for the default locale. The admin can
      // localize the options later.
      if (qd.getSupportedLocales().contains(LocalizedStrings.DEFAULT_LOCALE)) {
        qd.getOptionsForLocale(LocalizedStrings.DEFAULT_LOCALE).stream()
            .sorted(Comparator.comparing(LocalizedQuestionOption::order))
            .forEachOrdered(
                option -> {
                  options.add(option.optionText());
                  optionIds.add(option.id());
                });
        this.nextAvailableId =
            OptionalLong.of(
                qd.getOptionsForLocale(LocalizedStrings.DEFAULT_LOCALE).stream()
                        .mapToLong(LocalizedQuestionOption::id)
                        .max()
                        .getAsLong()
                    + 1);
      }
    } catch (TranslationNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public List<String> getOptions() {
    return this.options;
  }

  public List<String> getNewOptions() {
    return this.newOptions;
  }

  public List<Long> getOptionIds() {
    return this.optionIds;
  }

  public void setOptionIds(List<Long> optionIds) {
    this.optionIds = optionIds;
  }

  public void setNewOptions(List<String> options) {
    this.newOptions = options;
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

    ImmutableList.Builder<QuestionOption> questionOptions = ImmutableList.builder();
    Preconditions.checkState(
        this.optionIds.size() == this.options.size(),
        "Option ids and options are not the same size.");

    // Note: the question edit form only sets or updates the default locale.
    for (int i = 0; i < options.size(); i++) {
      questionOptions.add(
          QuestionOption.create(
              optionIds.get(i), i, LocalizedStrings.withDefaultValue(options.get(i))));
    }
    for (int i = 0; i < newOptions.size(); i++) {
      questionOptions.add(
          QuestionOption.create(
              nextAvailableId.orElse(0L) + i,
              options.size() + i,
              LocalizedStrings.withDefaultValue(newOptions.get(i))));
    }

    return super.getBuilder()
        .setQuestionOptions(questionOptions.build())
        .setValidationPredicates(predicateBuilder.build());
  }

  public OptionalLong getNextAvailableId() {
    return nextAvailableId;
  }

  public void setNextAvailableId(long nextAvailableId) {
    this.nextAvailableId = OptionalLong.of(nextAvailableId);
  }
}
