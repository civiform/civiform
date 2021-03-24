package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Locale;
import java.util.OptionalLong;
import services.Path;

public class SingleSelectQuestionDefinition extends QuestionDefinition {

  private final SingleSelectUiType singleSelectUiType;
  private final ImmutableListMultimap<Locale, String> options;

  public SingleSelectQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      SingleSelectUiType singleSelectUiType,
      ImmutableListMultimap<Locale, String> options) {
    super(
        id,
        version,
        name,
        path,
        description,
        questionText,
        questionHelpText,
        SingleSelectValidationPredicates.create());
    this.singleSelectUiType = singleSelectUiType;
    this.options = assertSameNumberOfOptionsForEachLocale(checkNotNull(options));
  }

  public SingleSelectQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      SingleSelectUiType singleSelectUiType,
      ImmutableListMultimap<Locale, String> options) {
    super(
        version,
        name,
        path,
        description,
        questionText,
        questionHelpText,
        SingleSelectValidationPredicates.create());
    this.singleSelectUiType = singleSelectUiType;
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
  public QuestionType getQuestionType() {
    return QuestionType.SINGLE_SELECT;
  }

  @Override
  public ImmutableMap<Path, ScalarType> getScalars() {
    return ImmutableMap.of(
        getSelectionPath(),
        getSelectionType(),
        getLastUpdatedTimePath(),
        getLastUpdatedTimeType(),
        getProgramIdPath(),
        getProgramIdType());
  }

  public Path getSelectionPath() {
    return getPath().toBuilder().append("selection").build();
  }

  public ScalarType getSelectionType() {
    return ScalarType.STRING;
  }

  public SingleSelectUiType getSingleSelectUiType() {
    return this.singleSelectUiType;
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

  /** The type of UI element that should be used to render this question. */
  public enum SingleSelectUiType {
    DROPDOWN,
    RADIO_BUTTON
  }

  @AutoValue
  public abstract static class SingleSelectValidationPredicates extends ValidationPredicates {
    public static SingleSelectValidationPredicates create() {
      return new AutoValue_SingleSelectQuestionDefinition_SingleSelectValidationPredicates();
    }
  }
}
