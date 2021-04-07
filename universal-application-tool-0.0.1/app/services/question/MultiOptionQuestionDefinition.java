package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import models.LifecycleStage;
import services.Path;

public abstract class MultiOptionQuestionDefinition extends QuestionDefinition {

  private final ImmutableListMultimap<Locale, String> options;

  protected MultiOptionQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      Optional<Long> repeaterId,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableListMultimap<Locale, String> options) {
    super(
        id,
        version,
        name,
        path,
        repeaterId,
        description,
        lifecycleStage,
        questionText,
        questionHelpText,
        MultiOptionValidationPredicates.create());
    this.options = assertSameNumberOfOptionsForEachLocale(checkNotNull(options));
  }

  protected MultiOptionQuestionDefinition(
      long version,
      String name,
      Path path,
      Optional<Long> repeaterId,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableListMultimap<Locale, String> options) {
    super(
        version,
        name,
        path,
        repeaterId,
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

  /**
   * Multi-option question type answers are strings. For questions that allow multiple answers (e.g.
   * checkbox questions), the type is still string, though a list is stored in the applicant JSON.
   */
  public ScalarType getSelectionType() {
    return ScalarType.STRING;
  }

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

  // TODO(https://github.com/seattle-uat/civiform/issues/416): Add logic for validation predicates -
  // this should have a min number of options and a max number of options.
  @AutoValue
  public abstract static class MultiOptionValidationPredicates extends ValidationPredicates {
    public static MultiOptionValidationPredicates create() {
      return new AutoValue_MultiOptionQuestionDefinition_MultiOptionValidationPredicates();
    }
  }
}
