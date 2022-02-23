package services.question.types;

import static com.google.common.base.Preconditions.checkNotNull;
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
import java.util.OptionalLong;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;

/** Superclass for all multi-option questions. */
public abstract class MultiOptionQuestionDefinition extends QuestionDefinition {

  protected static final MultiOptionValidationPredicates SINGLE_SELECT_PREDICATE =
      MultiOptionValidationPredicates.create(1, 1);

  private final ImmutableList<QuestionOption> options;
  private final ImmutableSet<Locale> supportedOptionLocales;

  protected MultiOptionQuestionDefinition(
      OptionalLong id,
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      ImmutableList<QuestionOption> options,
      MultiOptionValidationPredicates validationPredicates) {
    super(
        id, name, enumeratorId, description, questionText, questionHelpText, validationPredicates);
    this.options = checkNotNull(options);
    this.supportedOptionLocales = getSupportedOptionLocales(options);
  }

  protected MultiOptionQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      ImmutableList<QuestionOption> options,
      MultiOptionValidationPredicates validationPredicates) {
    super(name, enumeratorId, description, questionText, questionHelpText, validationPredicates);
    this.options = checkNotNull(options);
    this.supportedOptionLocales = getSupportedOptionLocales(options);
  }

  protected MultiOptionQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      ImmutableList<QuestionOption> options) {
    super(
        name,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        MultiOptionValidationPredicates.create());
    this.options = checkNotNull(options);
    this.supportedOptionLocales = getSupportedOptionLocales(options);
  }

  @Override
  public ImmutableSet<Locale> getSupportedLocales() {
    ImmutableSet<Locale> questionTextLocales = super.getSupportedLocales();
    return ImmutableSet.copyOf(Sets.intersection(questionTextLocales, this.supportedOptionLocales));
  }

  private ImmutableSet<Locale> getSupportedOptionLocales(ImmutableList<QuestionOption> options) {
    if (options.isEmpty()) {
      return ImmutableSet.of();
    }

    // The set of locales supported by a question's options is the smallest set of supported locales
    // for a single option
    ImmutableSet<ImmutableSet<Locale>> supportedLocales =
        options.stream()
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
    return this.options;
  }

  /**
   * Attempt to get question options for the given locale. If there aren't options for the given
   * locale, it will return the options for the default locale.
   */
  public ImmutableList<LocalizedQuestionOption> getOptionsForLocaleOrDefault(Locale locale) {
    return options.stream()
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
    if (supportedOptionLocales.contains(locale)) {
      return this.options.stream()
          .map(option -> option.localize(locale))
          .collect(toImmutableList());
    } else {
      // As in QuestionDefinition - we need to fetch "en_US" from "en".
      for (Locale supportedLocale : supportedOptionLocales) {
        if (supportedLocale.getLanguage().equals(locale.getLanguage())) {
          return this.options.stream()
              .map(option -> option.localize(supportedLocale))
              .collect(toImmutableList());
        }
      }
      throw new TranslationNotFoundException(locale);
    }
  }

  /** Get the default locale representation of the option with the given ID. */
  public Optional<String> getDefaultLocaleOptionForId(long id) {
    return getOptionsForDefaultLocale().stream()
        .filter(o -> o.id() == id)
        .map(LocalizedQuestionOption::optionText)
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
