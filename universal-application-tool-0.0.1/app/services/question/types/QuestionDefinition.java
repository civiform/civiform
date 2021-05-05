package services.question.types;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import services.CiviFormError;
import services.LocalizationUtils;
import services.Path;
import services.applicant.question.Scalar;
import services.question.exceptions.TranslationNotFoundException;

/** Defines a single question. */
public abstract class QuestionDefinition {
  private final OptionalLong id;
  private final String name;
  private final Path path;
  private final Optional<Long> enumeratorId;
  private final String description;
  // Note: you must check prefixes anytime you are doing a locale lookup
  // see getQuestionText body comment for explanation.
  private final ImmutableMap<Locale, String> questionText;
  private final ImmutableMap<Locale, String> questionHelpText;
  private final ValidationPredicates validationPredicates;

  public QuestionDefinition(
      OptionalLong id,
      String name,
      Path path,
      Optional<Long> enumeratorId,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ValidationPredicates validationPredicates) {
    this.id = checkNotNull(id);
    this.name = checkNotNull(name);
    this.path = checkNotNull(path);
    this.enumeratorId = checkNotNull(enumeratorId);
    this.description = checkNotNull(description);
    this.questionText = checkNotNull(questionText);
    this.questionHelpText = checkNotNull(questionHelpText);
    this.validationPredicates = checkNotNull(validationPredicates);
  }

  public QuestionDefinition(
      String name,
      Path path,
      Optional<Long> enumeratorId,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ValidationPredicates validationPredicates) {
    this(
        OptionalLong.empty(),
        name,
        path,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        validationPredicates);
  }

  public abstract static class ValidationPredicates {
    protected static final ObjectMapper mapper =
        new ObjectMapper().registerModule(new GuavaModule()).registerModule(new Jdk8Module());

    public String serializeAsString() {
      try {
        return mapper.writeValueAsString(this);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /** Return true if the question is persisted and has an unique identifier. */
  public boolean isPersisted() {
    return this.id.isPresent();
  }

  /** Get the unique identifier for this question. */
  public long getId() {
    return this.id.getAsLong();
  }

  /**
   * Get the name of this question.
   *
   * <p>Different versions of the same program are linked by their immutable name.
   *
   * <p>NOTE: This field will not be localized as it is for admin use only.
   */
  public String getName() {
    return this.name;
  }

  /** Returns the {@link Path} segment that corresponds to this QuestionDefinition. */
  public String getQuestionPathSegment() {
    // TODO(#783): Change this getter once we save this formatted name to the database.
    String formattedName = name.replaceAll("[^a-zA-Z ]", "").replaceAll("\\s", "_");
    if (getQuestionType().equals(QuestionType.ENUMERATOR)) {
      return formattedName + Path.ARRAY_SUFFIX;
    }
    return formattedName;
  }

  /**
   * A question is used to enumerate a variable list of user-defined identifiers for a repeated
   * entity (e.g. children, or household members).
   *
   * @return true if this is an enumerator question.
   */
  public boolean isEnumerator() {
    return getQuestionType().equals(QuestionType.ENUMERATOR);
  }

  /**
   * See {@link #getEnumeratorId()}.
   *
   * @return true if this is a repeated question.
   */
  public boolean isRepeated() {
    return enumeratorId.isPresent();
  }

  /**
   * A repeated question definition references an enumerator question definition that determines the
   * entities the repeated question definition asks its question for.
   *
   * <p>For example, the enumerator question "List your household members", may have a repeated
   * question asking for the birthdate of each household member. The repeated birthdate question
   * would have a reference to the household members enumerator question.
   *
   * <p>If a question definition does not have an enumeratorId, it is not repeated.
   *
   * @return the {@link QuestionDefinition#id} for this question definition's enumerator, if it
   *     exists.
   */
  public Optional<Long> getEnumeratorId() {
    return enumeratorId;
  }

  // TODO(https://github.com/seattle-uat/civiform/issues/673): delete this when question definitions
  //  don't need paths
  /** Get the full path of this question, in JSON notation. */
  public Path getPath() {
    return this.path;
  }

  /**
   * Get a human-readable description for the data this question collects.
   *
   * <p>NOTE: This field will not be localized as it is for admin use only.
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Attempts to get question text for the given locale. If there is no text for the given locale,
   * it will return the text in the default locale.
   */
  public String getQuestionTextOrDefault(Locale locale) {
    try {
      return getQuestionText(locale);
    } catch (TranslationNotFoundException e) {
      return getDefaultQuestionText();
    }
  }

  /** Gets the question text for CiviForm's default locale. */
  public String getDefaultQuestionText() {
    try {
      return getQuestionText(LocalizationUtils.DEFAULT_LOCALE);
    } catch (TranslationNotFoundException e) {
      // This should never happen - US English should always be supported.
      throw new RuntimeException(e);
    }
  }

  /**
   * Return an {@link Optional} containing the question text for this locale, or empty if this
   * locale is not supported.
   */
  public Optional<String> maybeGetQuestionText(Locale locale) {
    try {
      return Optional.of(getQuestionText(locale));
    } catch (TranslationNotFoundException e) {
      return Optional.empty();
    }
  }

  /** Get the question text for the given locale. */
  public String getQuestionText(Locale locale) throws TranslationNotFoundException {
    if (this.questionText.containsKey(locale)) {
      return this.questionText.get(locale);
    }
    // If we don't have the user's preferred locale, check if we have one which
    // contains their preferred language.  e.g. return "en_US" for "en_CA", or
    // "es_US" for "es_MX".  This is needed since some of our locale sources
    // provide only the language (e.g. "en").
    for (Locale hasLocale : this.questionText.keySet()) {
      if (hasLocale.getLanguage().equals(locale.getLanguage())) {
        return this.questionText.get(hasLocale);
      }
    }
    throw new TranslationNotFoundException(this.getPath().path(), locale);
  }

  /** Get the question tests for all locales. This is used for serialization. */
  public ImmutableMap<Locale, String> getQuestionText() {
    return questionText;
  }

  /**
   * Attempts to get the question help text for the given locale. If there is no help text localized
   * to the given locale, it will return text in the default locale.
   */
  public String getQuestionHelpTextOrDefault(Locale locale) {
    try {
      return getQuestionHelpText(locale);
    } catch (TranslationNotFoundException e) {
      return getDefaultQuestionHelpText();
    }
  }

  /**
   * Return an {@link Optional} containing the question help text for this locale, or empty if this
   * locale is not supported.
   */
  public Optional<String> maybeGetQuestionHelpText(Locale locale) {
    try {
      String helpText = getQuestionHelpText(locale);
      return helpText.isEmpty() ? Optional.empty() : Optional.of(helpText);
    } catch (TranslationNotFoundException e) {
      return Optional.empty();
    }
  }

  /** Gets the question help text for CiviForm's default locale. */
  public String getDefaultQuestionHelpText() {
    try {
      return getQuestionHelpText(LocalizationUtils.DEFAULT_LOCALE);
    } catch (TranslationNotFoundException e) {
      // This should never happen - US English should always be supported.
      throw new RuntimeException(e);
    }
  }

  /** Get the question help text for the given locale. */
  public String getQuestionHelpText(Locale locale) throws TranslationNotFoundException {
    if (this.questionHelpText.isEmpty()) {
      return "";
    }

    if (this.questionHelpText.containsKey(locale)) {
      return this.questionHelpText.get(locale);
    }
    // As in getQuestionText.
    for (Locale hasLocale : this.questionHelpText.keySet()) {
      if (hasLocale.getLanguage().equals(locale.getLanguage())) {
        return this.questionHelpText.get(hasLocale);
      }
    }

    throw new TranslationNotFoundException(this.getPath().path(), locale);
  }

  /** Get the question help text for all locales. This is used for serialization. */
  public ImmutableMap<Locale, String> getQuestionHelpText() {
    return this.questionHelpText;
  }

  /**
   * Get a set of {@link Locale}s that this question supports. A question fully supports a locale if
   * it provides translations for all applicant-visible text in that locale.
   */
  public ImmutableSet<Locale> getSupportedLocales() {
    return ImmutableSet.copyOf(
        Sets.intersection(this.questionText.keySet(), this.questionHelpText.keySet()));
  }

  /** Get the validation predicates. */
  public ValidationPredicates getValidationPredicates() {
    return validationPredicates;
  }

  /** Serialize validation predicates as a string. This is used for persisting in database. */
  public String getValidationPredicatesAsString() {
    return validationPredicates.serializeAsString();
  }

  /** Get the type of this question. */
  public abstract QuestionType getQuestionType();

  public Path getLastUpdatedTimePath() {
    return getPath().join(Scalar.UPDATED_AT);
  }

  public ScalarType getLastUpdatedTimeType() {
    return ScalarType.LONG;
  }

  public Path getProgramIdPath() {
    return getPath().join(Scalar.PROGRAM_UPDATED_IN);
  }

  public ScalarType getProgramIdType() {
    return ScalarType.LONG;
  }

  /** Get a map of all scalars stored by this question definition. */
  public ImmutableMap<Path, ScalarType> getScalars() {
    return ImmutableMap.<Path, ScalarType>builder()
        .putAll(getScalarMap())
        .putAll(getMetadataMap())
        .build();
  }

  /** Get a map of question specific scalars stored by this question definition. */
  abstract ImmutableMap<Path, ScalarType> getScalarMap();

  /** Get a map of metadata stored by all question definitions. */
  ImmutableMap<Path, ScalarType> getMetadataMap() {
    return ImmutableMap.of(
        getLastUpdatedTimePath(), getLastUpdatedTimeType(), getProgramIdPath(), getProgramIdType());
  }

  /** Validate that all required fields are present and valid for the question. */
  public ImmutableSet<CiviFormError> validate() {
    ImmutableSet.Builder<CiviFormError> errors = new ImmutableSet.Builder<>();
    if (name.isBlank()) {
      errors.add(CiviFormError.of("Name cannot be blank"));
    }
    if (description.isBlank()) {
      errors.add(CiviFormError.of("Description cannot be blank"));
    }
    if (questionText.isEmpty()) {
      errors.add(CiviFormError.of("Question text cannot be blank"));
    }
    if (questionText.values().stream().anyMatch(String::isBlank)) {
      errors.add(CiviFormError.of("Question text cannot be blank"));
    }
    return errors.build();
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, path);
  }

  /** Two QuestionDefinitions are considered equal if all of their properties are the same. */
  @Override
  public boolean equals(Object other) {
    return this.idEquals(other) && this.equalsIgnoreId(other);
  }

  public boolean idEquals(Object other) {
    if (other instanceof QuestionDefinition) {
      QuestionDefinition o = (QuestionDefinition) other;

      return this.isPersisted() == o.isPersisted()
          && (!this.isPersisted() || this.getId() == o.getId());
    }

    return false;
  }

  /**
   * When an object is created, it is sent to the server without an id. The object returned from
   * QuestionService should be the QuestionDefinition with the id.
   *
   * <p>This checks all other fields ignoring the id.
   */
  public boolean equalsIgnoreId(Object other) {
    if (other instanceof QuestionDefinition) {
      QuestionDefinition o = (QuestionDefinition) other;

      return this.getQuestionType().equals(o.getQuestionType())
          && this.name.equals(o.getName())
          && this.path.equals(o.getPath())
          && this.description.equals(o.getDescription())
          && this.questionText.equals(o.getQuestionText())
          && this.questionHelpText.equals(o.getQuestionHelpText())
          && this.validationPredicates.equals(o.getValidationPredicates());
    }
    return false;
  }
}
