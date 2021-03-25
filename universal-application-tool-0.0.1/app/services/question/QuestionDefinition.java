package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import services.CiviFormError;
import services.Path;

/** Defines a single question. */
public abstract class QuestionDefinition {
  public static final String METADATA_UPDATE_TIME_KEY = "updated_at";
  public static final String METADATA_UPDATE_PROGRAM_ID_KEY = "updated_in_program";

  private final OptionalLong id;
  private final long version;
  private final String name;
  private final Path path;
  private final String description;
  private final ImmutableMap<Locale, String> questionText;
  private final ImmutableMap<Locale, String> questionHelpText;
  private final ValidationPredicates validationPredicates;

  public QuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ValidationPredicates validationPredicates) {
    this.id = checkNotNull(id);
    this.version = version;
    this.name = checkNotNull(name);
    this.path = checkNotNull(path);
    this.description = checkNotNull(description);
    this.questionText = checkNotNull(questionText);
    this.questionHelpText = checkNotNull(questionHelpText);
    this.validationPredicates = checkNotNull(validationPredicates);
  }

  public QuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ValidationPredicates validationPredicates) {
    this(
        OptionalLong.empty(),
        version,
        name,
        path,
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

  /** Get the system version this question is pinned to. */
  public long getVersion() {
    return this.version;
  }

  /**
   * Get the name of this question.
   *
   * <p>NOTE: This field will not be localized as it is for admin use only.
   */
  public String getName() {
    return this.name;
  }

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

  /** Get the question help text for the given locale. */
  public String getQuestionHelpText(Locale locale) throws TranslationNotFoundException {
    if (this.questionHelpText.isEmpty()) {
      return "";
    }

    if (this.questionHelpText.containsKey(locale)) {
      return this.questionHelpText.get(locale);
    }

    throw new TranslationNotFoundException(this.getPath().path(), locale);
  }

  /** Get the question help text for all locales. This is used for serialization. */
  public ImmutableMap<Locale, String> getQuestionHelpText() {
    return this.questionHelpText;
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
    return getPath().toBuilder().append(METADATA_UPDATE_TIME_KEY).build();
  }

  public ScalarType getLastUpdatedTimeType() {
    return ScalarType.LONG;
  }

  public Path getProgramIdPath() {
    return getPath().toBuilder().append(METADATA_UPDATE_PROGRAM_ID_KEY).build();
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

  public Optional<ScalarType> getScalarType(Path path) {
    return Optional.ofNullable(this.getScalars().get(path));
  }

  /** Validate that all required fields are present and valid for the question. */
  public ImmutableSet<CiviFormError> validate() {
    ImmutableSet.Builder<CiviFormError> errors = new ImmutableSet.Builder<CiviFormError>();
    if (version < 1) {
      errors.add(CiviFormError.of(String.format("invalid version: %d", version)));
    }
    if (name.isBlank()) {
      errors.add(CiviFormError.of("blank name"));
    }
    if (!hasValidPathPattern()) {
      errors.add(CiviFormError.of(String.format("invalid path pattern: '%s'", path.path())));
    }
    if (description.isBlank()) {
      errors.add(CiviFormError.of("blank description"));
    }
    if (questionText.isEmpty()) {
      errors.add(CiviFormError.of("no question text"));
    }
    return errors.build();
  }

  private boolean hasValidPathPattern() {
    if (path.path().isBlank()) {
      return false;
    }
    return URLEncoder.encode(path.path(), StandardCharsets.UTF_8).equals(path.path());
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, version, path);
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
          && this.version == o.getVersion()
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
