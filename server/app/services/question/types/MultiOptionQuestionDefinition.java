package services.question.types;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import services.CiviFormError;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;

/** Superclass for all multi-option questions. */
public final class MultiOptionQuestionDefinition extends QuestionDefinition {

  public enum MultiOptionQuestionType {
    CHECKBOX,
    DROPDOWN,
    RADIO_BUTTON
  }

  private static final MultiOptionValidationPredicates SINGLE_SELECT_PREDICATE =
      MultiOptionValidationPredicates.create(1, 1);
  private final ImmutableList<QuestionOption> questionOptions;
  private final MultiOptionQuestionType multiOptionQuestionType;

  public MultiOptionQuestionDefinition(
      QuestionDefinitionConfig questionDefinitionConfig,
      ImmutableList<QuestionOption> questionOptions,
      MultiOptionQuestionType multiOptionQuestionType) {
    super(fixValidationPredicates(questionDefinitionConfig, multiOptionQuestionType));
    this.questionOptions = questionOptions;
    this.multiOptionQuestionType = multiOptionQuestionType;
  }

  // If we are using a dropdown or radio button, set the SINGLE_SELECT_PREDICATE to ensure
  // only one selection can be made.
  private static QuestionDefinitionConfig fixValidationPredicates(
      QuestionDefinitionConfig config, MultiOptionQuestionType multiOptionQuestionType) {
    QuestionDefinitionConfig.Builder builder = config.toBuilder();

    if (multiOptionQuestionType == MultiOptionQuestionType.DROPDOWN
        || multiOptionQuestionType == MultiOptionQuestionType.RADIO_BUTTON) {
      builder.setValidationPredicates(SINGLE_SELECT_PREDICATE);
    }

    return builder.build();
  }

  @Override
  public QuestionType getQuestionType() {
    switch (multiOptionQuestionType) {
      case CHECKBOX:
        return QuestionType.CHECKBOX;
      case DROPDOWN:
        return QuestionType.DROPDOWN;
      case RADIO_BUTTON:
        return QuestionType.RADIO_BUTTON;
    }
    throw new IllegalStateException("Not a valid MultiOptionQuestionType.");
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return MultiOptionValidationPredicates.create();
  }

  @Override
  public ImmutableSet<Locale> getSupportedLocales() {
    ImmutableSet<Locale> questionTextLocales = super.getSupportedLocales();
    return ImmutableSet.copyOf(Sets.intersection(questionTextLocales, getSupportedOptionLocales()));
  }

  @Override
  ImmutableSet<CiviFormError> internalValidate(Optional<QuestionDefinition> previousDefinition) {
    ImmutableSet.Builder<CiviFormError> errors = new ImmutableSet.Builder<>();
    if (getOptions().isEmpty()) {
      errors.add(CiviFormError.of("Multi-option questions must have at least one option"));
    }

    if (getOptionAdminNames().stream().anyMatch(String::isEmpty)) {
      errors.add(CiviFormError.of("Multi-option questions cannot have blank admin names"));
    }

    var existingAdminNames =
        previousDefinition
            .map(qd -> (MultiOptionQuestionDefinition) qd)
            .map(MultiOptionQuestionDefinition::getOptionAdminNames)
            .orElse(ImmutableList.of())
            // New option admin names can only be lowercase, but existing option
            // admin names may have capital letters. We lowercase them before
            // comparing to avoid collisions.
            .stream()
            .map(o -> o.toLowerCase(Locale.ROOT))
            .collect(ImmutableList.toImmutableList());

    if (getOptionAdminNames().stream()
        // This is O(n^2) but the list is small and it's simpler than creating a Set
        .filter(n -> !existingAdminNames.contains(n.toLowerCase(Locale.ROOT)))
        .anyMatch(s -> !s.matches("[0-9a-z_-]+"))) {
      errors.add(
          CiviFormError.of(
              "Multi-option admin names can only contain lowercase letters, numbers, underscores,"
                  + " and dashes"));
    }
    if (getOptions().stream().anyMatch(option -> option.optionText().hasEmptyTranslation())) {
      errors.add(CiviFormError.of("Multi-option questions cannot have blank options"));
    }

    int numOptions = getOptions().size();
    long numUniqueOptionDefaultValues =
        getOptions().stream()
            .map(QuestionOption::optionText)
            .map(e -> e.getDefault().toLowerCase(Locale.ROOT))
            .distinct()
            .count();
    if (numUniqueOptionDefaultValues != numOptions) {
      errors.add(CiviFormError.of("Multi-option question options must be unique"));
    }

    long numUniqueOptionAdminNames =
        getOptions().stream()
            .map(QuestionOption::adminName)
            .map(e -> e.toLowerCase(Locale.ROOT))
            .distinct()
            .count();
    if (numUniqueOptionAdminNames != numOptions) {
      errors.add(CiviFormError.of("Multi-option question admin names must be unique"));
    }

    OptionalInt minChoicesRequired = getMultiOptionValidationPredicates().minChoicesRequired();
    OptionalInt maxChoicesAllowed = getMultiOptionValidationPredicates().maxChoicesAllowed();
    if (minChoicesRequired.isPresent()) {
      if (minChoicesRequired.getAsInt() < 0) {
        errors.add(CiviFormError.of("Minimum number of choices required cannot be negative"));
      }

      if (minChoicesRequired.getAsInt() > numOptions) {
        errors.add(
            CiviFormError.of(
                "Minimum number of choices required cannot exceed the number of options"));
      }
    }

    if (maxChoicesAllowed.isPresent()) {
      if (maxChoicesAllowed.getAsInt() < 0) {
        errors.add(CiviFormError.of("Maximum number of choices allowed cannot be negative"));
      }

      if (maxChoicesAllowed.getAsInt() > numOptions) {
        errors.add(
            CiviFormError.of(
                "Maximum number of choices allowed cannot exceed the number of options"));
      }
    }

    if (minChoicesRequired.isPresent() && maxChoicesAllowed.isPresent()) {
      if (minChoicesRequired.getAsInt() == 0 && maxChoicesAllowed.getAsInt() == 0) {
        errors.add(CiviFormError.of("Cannot require exactly 0 choices"));
      }

      if (minChoicesRequired.getAsInt() > maxChoicesAllowed.getAsInt()) {
        errors.add(
            CiviFormError.of(
                "Minimum number of choices required must be less than or equal to the maximum"
                    + " choices allowed"));
      }
    }

    return errors.build();
  }

  private ImmutableSet<Locale> getSupportedOptionLocales() {
    if (questionOptions.isEmpty()) {
      return ImmutableSet.of();
    }

    // The set of locales supported by a question's options is the smallest set of supported locales
    // for a single option
    ImmutableSet<ImmutableSet<Locale>> supportedLocales =
        questionOptions.stream()
            .map(option -> option.optionText().translations().keySet())
            .collect(toImmutableSet());

    Optional<ImmutableSet<Locale>> smallestSet =
        supportedLocales.stream().reduce((a, b) -> a.size() < b.size() ? a : b);

    if (smallestSet.isEmpty()) {
      // This should never happen - we should always have at least one option at this point.
      throw new RuntimeException("Could not determine supported option locales for question");
    }

    return smallestSet.get();
  }

  public ImmutableList<QuestionOption> getOptions() {
    return this.questionOptions;
  }

  /**
   * Get the admin names of the question's options.
   *
   * @return a list of option admin names.
   */
  public ImmutableList<String> getOptionAdminNames() {
    return this.questionOptions.stream().map(QuestionOption::adminName).collect(toImmutableList());
  }

  /**
   * Attempt to get question options for the given locale. If there aren't options for the given
   * locale, it will return the options for the default locale.
   */
  public ImmutableList<LocalizedQuestionOption> getOptionsForLocaleOrDefault(Locale locale) {
    return questionOptions.stream()
        .map(option -> option.localizeOrDefault(locale))
        .collect(toImmutableList());
  }

  /** Get question options localized to CiviForm's default locale. */
  public ImmutableList<LocalizedQuestionOption> getOptionsForDefaultLocale() {
    try {
      return getOptionsForLocale(LocalizedStrings.DEFAULT_LOCALE);
    } catch (TranslationNotFoundException e) {
      // This should never happen - there should always be options localized to the default locale.
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the ordered list of options for this {@link Locale}, if it exists. Throws an exception if
   * localized options do not exist for this locale.
   *
   * @param locale the {@link Locale} to find
   * @return the list of options localized for the given {@link Locale}
   * @throws TranslationNotFoundException if there are no localized options for the given {@link
   *     Locale}
   */
  public ImmutableList<LocalizedQuestionOption> getOptionsForLocale(Locale locale)
      throws TranslationNotFoundException {
    if (getSupportedOptionLocales().contains(locale)) {
      return this.questionOptions.stream()
          .map(option -> option.localize(locale))
          .collect(toImmutableList());
    } else {
      // As in QuestionDefinition - we need to fetch "en_US" from "en".
      for (Locale supportedLocale : getSupportedOptionLocales()) {
        if (supportedLocale.getLanguage().equals(locale.getLanguage())) {
          return this.questionOptions.stream()
              .map(option -> option.localize(supportedLocale))
              .collect(toImmutableList());
        }
      }
      throw new TranslationNotFoundException(locale);
    }
  }

  /** Get the admin name representation of the option with the given ID. */
  public Optional<String> getOptionAdminNameForId(long id) {
    return getOptions().stream()
        .filter(o -> o.id() == id)
        .map(QuestionOption::adminName)
        .findFirst();
  }

  public MultiOptionValidationPredicates getMultiOptionValidationPredicates() {
    return (MultiOptionValidationPredicates) getValidationPredicates();
  }

  @JsonDeserialize(
      builder =
          AutoValue_MultiOptionQuestionDefinition_MultiOptionValidationPredicates.Builder.class)
  @AutoValue
  public abstract static class MultiOptionValidationPredicates extends ValidationPredicates {

    public static MultiOptionValidationPredicates create() {
      return builder().build();
    }

    public static MultiOptionValidationPredicates create(
        int minChoicesRequired, int maxChoicesAllowed) {
      return builder()
          .setMinChoicesRequired(minChoicesRequired)
          .setMaxChoicesAllowed(maxChoicesAllowed)
          .build();
    }

    public static MultiOptionValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString,
            AutoValue_MultiOptionQuestionDefinition_MultiOptionValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static Builder builder() {
      return new AutoValue_MultiOptionQuestionDefinition_MultiOptionValidationPredicates.Builder();
    }

    @JsonProperty("minChoicesRequired")
    public abstract OptionalInt minChoicesRequired();

    @JsonProperty("maxChoicesAllowed")
    public abstract OptionalInt maxChoicesAllowed();

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("minChoicesRequired")
      public abstract Builder setMinChoicesRequired(OptionalInt minChoicesRequired);

      public abstract Builder setMinChoicesRequired(int minChoicesRequired);

      @JsonProperty("maxChoicesAllowed")
      public abstract Builder setMaxChoicesAllowed(OptionalInt maxChoicesAllowed);

      public abstract Builder setMaxChoicesAllowed(int maxChoicesAllowed);

      public abstract MultiOptionValidationPredicates build();
    }
  }
}
