package forms.questions;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import services.CiviFormError;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

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
  // Optional per-option scores, parallel to options/newOptions. Stored as strings so that blank
  // (unscored), 0, and invalid input are distinguishable; see parseScore.
  private List<String> optionScores;
  private List<String> newOptionScores;

  // The IDs of option types which have been selected by the admin to be included in the question's
  // answer options.
  // This list is currently only applicable to YES_NO questions, which contain optional question
  // options.
  private List<Long> displayedOptionIds;

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
    this.optionScores = new ArrayList<>();
    this.newOptionScores = new ArrayList<>();
    this.displayedOptionIds = new ArrayList<>();
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
    this.optionScores = new ArrayList<>();
    this.newOptionScores = new ArrayList<>();
    this.displayedOptionIds = new ArrayList<>();

    // Scores live on QuestionOption only (never on the applicant-facing LocalizedQuestionOption),
    // so resolve them by option id.
    ImmutableMap<Long, QuestionOption> optionsById =
        qd.getOptions().stream()
            .collect(
                ImmutableMap.toImmutableMap(QuestionOption::id, option -> option, (a, b) -> a));

    try {
      // The first time a question is created, we only create for the default locale. The admin can
      // localize the options later.
      if (qd.getSupportedLocales().contains(LocalizedStrings.DEFAULT_LOCALE)) {
        qd.getOptionsForLocale(LocalizedStrings.DEFAULT_LOCALE).stream()
            .sorted(Comparator.comparingLong(LocalizedQuestionOption::order))
            .forEachOrdered(
                option -> {
                  options.add(option.optionText());
                  optionIds.add(option.id());
                  optionAdminNames.add(option.adminName());
                  optionScores.add(
                      Optional.ofNullable(optionsById.get(option.id()))
                          .flatMap(QuestionOption::score)
                          .map(QuestionOption::formatScore)
                          .orElse(""));
                  if (getQuestionType() == QuestionType.YES_NO) {
                    if (option.displayInAnswerOptions().isPresent()
                        && option.displayInAnswerOptions().get()) {
                      displayedOptionIds.add(option.id());
                    }
                  }
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

  public List<String> getOptionScores() {
    return this.optionScores;
  }

  public void setOptionScores(List<String> optionScores) {
    this.optionScores = optionScores;
  }

  public List<String> getNewOptionScores() {
    return this.newOptionScores;
  }

  public void setNewOptionScores(List<String> newOptionScores) {
    this.newOptionScores = newOptionScores;
  }

  public List<Long> getDisplayedOptionIds() {
    return this.displayedOptionIds;
  }

  public void setDisplayedOptionIds(List<Long> displayedOptionIds) {
    this.displayedOptionIds = displayedOptionIds;
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
   * Returns validation problems with the submitted option scores, as form errors rather than
   * exceptions so the edit view can re-render with a message.
   *
   * <p>Each score list must be parallel to its option list, and every non-blank entry must parse
   * as a finite decimal number. The cardinality check is unconditional: callers only validate
   * scores when the score inputs were rendered, so a missing or short list is a crafted post (or
   * a mid-edit flag flip) and must error rather than silently build unscored options — for an
   * existing question that would wipe its stored scores.
   */
  public ImmutableSet<CiviFormError> getOptionScoreErrors() {
    ImmutableSet.Builder<CiviFormError> errors = ImmutableSet.builder();
    if (optionScores.size() != options.size()) {
      errors.add(
          CiviFormError.of("The number of option scores does not match the number of options"));
    }
    if (newOptionScores.size() != newOptions.size()) {
      errors.add(
          CiviFormError.of(
              "The number of new option scores does not match the number of new options"));
    }
    for (String scoreAsString : optionScores) {
      addParseErrorIfInvalid(errors, scoreAsString);
    }
    for (String scoreAsString : newOptionScores) {
      addParseErrorIfInvalid(errors, scoreAsString);
    }
    return errors.build();
  }

  private static void addParseErrorIfInvalid(
      ImmutableSet.Builder<CiviFormError> errors, String scoreAsString) {
    if (!isBlank(scoreAsString) && parseScore(scoreAsString).isEmpty()) {
      errors.add(
          CiviFormError.of(
              String.format("Option score '%s' must be a number", scoreAsString.trim())));
    }
  }

  private static boolean isBlank(String scoreAsString) {
    return scoreAsString == null || scoreAsString.isBlank();
  }

  /**
   * Parses an admin-entered score. Blank means unscored. Parsing goes through {@link BigDecimal}
   * rather than {@link Double#parseDouble} as a server-side backstop against crafted posts: it
   * accepts plain and exponent decimal notation but rejects the NaN/Infinity/hex/suffix forms
   * Double.parseDouble tolerates, and the finite check rejects double-overflowing exponents.
   * Invalid input surfaces as empty here and as errors in {@link #getOptionScoreErrors}.
   */
  private static Optional<Double> parseScore(String scoreAsString) {
    if (isBlank(scoreAsString)) {
      return Optional.empty();
    }
    try {
      double score = new BigDecimal(scoreAsString.trim()).doubleValue();
      return Double.isFinite(score) ? Optional.of(score) : Optional.empty();
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  private static Optional<Double> scoreAt(List<String> scores, int index) {
    return index < scores.size() ? parseScore(scores.get(index)) : Optional.empty();
  }

  /**
   * Build a {@link QuestionDefinitionBuilder} from this QuestionForm, for handling the form
   * response. Option scores are never applied through this overload; callers with the scoring
   * feature flag in hand use {@link #getBuilder(boolean)}.
   *
   * @return a {@link QuestionDefinitionBuilder} with the values from this QuestionForm
   */
  @Override
  public QuestionDefinitionBuilder getBuilder() {
    return getBuilder(/* scoringEnabled= */ false);
  }

  /**
   * Build a {@link QuestionDefinitionBuilder} from this QuestionForm, for handling the form
   * response.
   *
   * @param scoringEnabled whether the answer-option-scoring feature flag is on for this request;
   *     when false (or the question type does not support scores), submitted scores are discarded
   *     and options are built unscored
   * @return a {@link QuestionDefinitionBuilder} with the values from this QuestionForm
   */
  public QuestionDefinitionBuilder getBuilder(boolean scoringEnabled) {
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

    // Scores only apply when the feature flag is on and the type supports them; this inherently
    // excludes Yes/No questions.
    boolean applyScores = scoringEnabled && QuestionType.supportsOptionScores(getQuestionType());

    // Note: the question edit form only sets or updates the default locale.
    for (int i = 0; i < options.size(); i++) {
      // Yes/No questions have optional question options; all other question types should write
      // "true" for displayInAnswerOptions.
      boolean displayInAnswerOptions =
          getQuestionType() == QuestionType.YES_NO
              ? displayedOptionIds.contains(optionIds.get(i))
              : true;
      questionOptionsBuilder.add(
          QuestionOption.create(
              /* id= */ optionIds.get(i),
              /* displayOrder= */ i,
              /* adminName= */ optionAdminNames.get(i),
              /* optionText= */ LocalizedStrings.withDefaultValue(options.get(i)),
              /* displayInAnswerOptions= */ Optional.of(displayInAnswerOptions),
              /* score= */ applyScores ? scoreAt(optionScores, i) : Optional.empty()));
    }

    // Get the next available ID, from either the max of the option IDs in the response or the
    // nextAvailableId in the response
    Long maxIdInFormResponseOptions = optionIds.stream().max(Long::compareTo).orElse(-1L);
    setNextAvailableId(Math.max(nextAvailableId.orElse(0), maxIdInFormResponseOptions + 1));

    for (int i = 0; i < newOptions.size(); i++) {
      questionOptionsBuilder.add(
          QuestionOption.create(
              /* id= */ nextAvailableId.getAsLong() + i,
              /* displayOrder= */ options.size() + i,
              /* adminName= */ newOptionAdminNames.get(i),
              /* optionText= */ LocalizedStrings.withDefaultValue(newOptions.get(i)),
              /* displayInAnswerOptions= */ Optional.of(true),
              /* score= */ applyScores ? scoreAt(newOptionScores, i) : Optional.empty()));
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
