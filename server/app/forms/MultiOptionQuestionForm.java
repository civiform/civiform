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
  // The IDs of each option are not expected to be in any particular order.
  private List<Long> optionIds;
  private List<String> optionAdminNames;
  private List<String> newOptionAdminNames;
  // This value is the max existing ID + 1. The max ID will not necessarily be the last one in the
  // optionIds list, we do not store options by order of their IDs.
  private OptionalLong nextAvailableId;
  private OptionalInt minChoicesRequired;
  private OptionalInt maxChoicesAllowed;

  protected MultiOptionQuestionForm() {
    super();
    this.options = new ArrayList<>();
    this.newOptions = new ArrayList<>();
    this.optionIds = new ArrayList<>();
    this.optionAdminNames = new ArrayList<>();
    this.newOptionAdminNames = new ArrayList<>();
    this.minChoicesRequired = OptionalInt.empty();
    this.maxChoicesAllowed = OptionalInt.empty();
    this.nextAvailableId = OptionalLong.of(0);
  }

  /**
   * Build a QuestionForm from a {@link QuestionDefinition}, to build the QuestionEditView.
   *
   * @param qd the {@link QuestionDefinition} from which to build a QuestionForm
   */
  protected MultiOptionQuestionForm(MultiOptionQuestionDefinition qd) {
    super(qd);
    this.minChoicesRequired = qd.getMultiOptionValidationPredicates().minChoicesRequired();
    this.maxChoicesAllowed = qd.getMultiOptionValidationPredicates().maxChoicesAllowed();

    this.options = new ArrayList<>();
    this.newOptions = new ArrayList<>();
    this.optionIds = new ArrayList<>();
    this.optionAdminNames = new ArrayList<>();
    this.newOptionAdminNames = new ArrayList<>();

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
                  optionAdminNames.add(option.adminName());
                });
        this.nextAvailableId =
            OptionalLong.of(
                qd.getOptionsForLocale(LocalizedStrings.DEFAULT_LOCALE).stream()
                        .mapToLong(LocalizedQuestionOption::id)
                        .max()
                        .orElse(0)
                    + 1);
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

  public List<String> getNewOptions() {
    return this.newOptions;
  }

  public void setNewOptions(List<String> options) {
    this.newOptions = options;
  }

  public List<Long> getOptionIds() {
    return this.optionIds;
  }

  public void setOptionIds(List<Long> optionIds) {
    this.optionIds = optionIds;
  }

  public List<String> getOptionAdminNames() {
    return this.optionAdminNames;
  }

  public void setOptionAdminNames(List<String> optionAdminNames) {
    this.optionAdminNames = optionAdminNames;
  }

  public List<String> getNewOptionAdminNames() {
    return this.newOptionAdminNames;
  }

  public void setNewOptionAdminNames(List<String> newOptionAdminNames) {
    this.newOptionAdminNames = newOptionAdminNames;
  }

  public OptionalInt getMinChoicesRequired() {
    return minChoicesRequired;
  }

  public OptionalLong getNextAvailableId() {
    return nextAvailableId;
  }

  public void setNextAvailableId(long nextAvailableId) {
    this.nextAvailableId = OptionalLong.of(nextAvailableId);
  }

  /**
   * We use a string parameter here so that if the field is empty (i.e. unset), we can correctly set
   * to an empty OptionalInt. Since the HTML input is type "number", we can be sure this string is
   * in fact an integer when we parse it. If we instead used an int here, we see an "Invalid value"
   * error when binding the empty value in the form.
   */
  public void setMinChoicesRequired(String minChoicesRequiredAsString) {
    this.minChoicesRequired =
        minChoicesRequiredAsString.isEmpty()
            ? OptionalInt.empty()
            : OptionalInt.of(Integer.parseInt(minChoicesRequiredAsString));
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
    this.maxChoicesAllowed =
        maxChoicesAllowedAsString.isEmpty()
            ? OptionalInt.empty()
            : OptionalInt.of(Integer.parseInt(maxChoicesAllowedAsString));
  }

  /**
   * Build a {@link QuestionDefinitionBuilder} from this QuestionForm, for handling the form
   * response.
   *
   * @return a {@link QuestionDefinitionBuilder} with the values from this QuestionForm
   */
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

    ImmutableList.Builder<QuestionOption> questionOptionsBuilder = ImmutableList.builder();
    Preconditions.checkState(
        this.optionIds.size() == this.options.size(),
        "Option ids and options are not the same size.");
    Preconditions.checkState(
        this.optionAdminNames.size() == this.options.size(),
        "Option admin names and options are not the same size.");

    // Note: the question edit form only sets or updates the default locale.
    for (int i = 0; i < options.size(); i++) {
      questionOptionsBuilder.add(
          QuestionOption.create(
              optionIds.get(i),
              i,
              optionAdminNames.get(i),
              LocalizedStrings.withDefaultValue(options.get(i))));
    }

    // Get the next available ID, from either the max of the option IDs in the response or the
    // nextAvailableId in the response
    Long maxIdInFormResponseOptions = optionIds.stream().max(Long::compareTo).orElse(-1L);
    setNextAvailableId(Math.max(nextAvailableId.orElse(0), maxIdInFormResponseOptions + 1));

    for (int i = 0; i < newOptions.size(); i++) {
      questionOptionsBuilder.add(
          QuestionOption.create(
              nextAvailableId.getAsLong() + i,
              options.size() + i,
              newOptionAdminNames.get(i),
              LocalizedStrings.withDefaultValue(newOptions.get(i))));
    }
    ImmutableList<QuestionOption> questionOptions = questionOptionsBuilder.build();

    // Sets the next available ID as the previous ID + the size of new options, since each new
    // option ID is assigned in order.
    setNextAvailableId(nextAvailableId.getAsLong() + newOptions.size());

    return super.getBuilder()
        .setQuestionOptions(questionOptions)
        .setValidationPredicates(predicateBuilder.build());
  }
}
