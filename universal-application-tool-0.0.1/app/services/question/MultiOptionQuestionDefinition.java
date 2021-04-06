package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.OptionalLong;
import models.LifecycleStage;
import services.Path;

public abstract class MultiOptionQuestionDefinition extends QuestionDefinition {

  protected static final MultiOptionValidationPredicates SINGLE_SELECT_PREDICATE =
      MultiOptionValidationPredicates.create(1, 1);

  private final ImmutableListMultimap<Locale, String> options;

  protected MultiOptionQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableListMultimap<Locale, String> options,
      MultiOptionValidationPredicates validationPredicates) {
    super(
        id,
        version,
        name,
        path,
        description,
        lifecycleStage,
        questionText,
        questionHelpText,
        validationPredicates);
    this.options = assertSameNumberOfOptionsForEachLocale(checkNotNull(options));
  }

  protected MultiOptionQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableListMultimap<Locale, String> options,
      MultiOptionValidationPredicates validationPredicates) {
    super(
        version,
        name,
        path,
        description,
        lifecycleStage,
        questionText,
        questionHelpText,
        validationPredicates);
    this.options = assertSameNumberOfOptionsForEachLocale(checkNotNull(options));
  }

  protected MultiOptionQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableListMultimap<Locale, String> options) {
    super(
        version,
        name,
        path,
        description,
        lifecycleStage,
        questionText,
        questionHelpText,
        MultiOptionValidationPredicates.create());
    this.options = assertSameNumberOfOptionsForEachLocale(checkNotNull(options));
  }

  private ImmutableListMultimap<Locale, String> assertSameNumberOfOptionsForEachLocale(
      ImmutableListMultimap<Locale, String> options) {
    long numDistinctLists =
        options.asMap().values().stream().mapToInt(Collection::size).distinct().count();
    if (numDistinctLists <= 1) {
      return options;
    }
    throw new RuntimeException("The lists of options are not the same for all locales");
  }

  @Override
  public ImmutableMap<Path, ScalarType> getScalarMap() {
    return ImmutableMap.of(getSelectionPath(), getSelectionType());
  }

  public Path getSelectionPath() {
    return getPath().join("selection");
  }

  public abstract ScalarType getSelectionType();

  public ImmutableListMultimap<Locale, String> getOptions() {
    return this.options;
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
  public ImmutableList<String> getOptionsForLocale(Locale locale)
      throws TranslationNotFoundException {
    if (this.options.containsKey(locale)) {
      return this.options.get(locale);
    } else {
      throw new TranslationNotFoundException(getPath().toString(), locale);
    }
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

    @JsonProperty("minChoicesRequired")
    public abstract OptionalInt minChoicesRequired();

    @JsonProperty("maxChoicesAllowed")
    public abstract OptionalInt maxChoicesAllowed();

    public static Builder builder() {
      return new AutoValue_MultiOptionQuestionDefinition_MultiOptionValidationPredicates.Builder();
    }

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
