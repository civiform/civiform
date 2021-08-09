package services.question.types;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import services.CiviFormError;
import services.LocalizedStrings;
import services.Path;
import services.applicant.RepeatedEntity;
import services.question.QuestionOption;

/** Superclass for all question types. */
public abstract class QuestionDefinition {
  private final OptionalLong id;
  private final String name;
  private final Optional<Long> enumeratorId;
  private final String description;
  // Note: you must check prefixes anytime you are doing a locale lookup
  // see getQuestionText body comment for explanation.
  private final LocalizedStrings questionText;
  private final LocalizedStrings questionHelpText;
  private final ValidationPredicates validationPredicates;

  public QuestionDefinition(
      OptionalLong id,
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      ValidationPredicates validationPredicates) {
    this.id = checkNotNull(id);
    this.name = checkNotNull(name);
    this.enumeratorId = checkNotNull(enumeratorId);
    this.description = checkNotNull(description);
    this.questionText = checkNotNull(questionText);
    this.questionHelpText = checkNotNull(questionHelpText);
    this.validationPredicates = checkNotNull(validationPredicates);
  }

  public QuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      ValidationPredicates validationPredicates) {
    this(
        OptionalLong.empty(),
        name,
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
   * Returns the contextualized path for this question. The path is contextualized with respect to
   * the enumerated elements it is about. If there is no repeated entity for context, the {@code
   * defaultRoot} is used.
   *
   * <p>For example, a generic path about the name of an applicant's household member may look like
   * "root.household_member[].name", while a contextualized path would look like
   * "root.household_member[3].name".
   */
  public Path getContextualizedPath(Optional<RepeatedEntity> repeatedEntity, Path defaultRoot) {
    return repeatedEntity
        .map(RepeatedEntity::contextualizedPath)
        .orElse(defaultRoot)
        .join(getQuestionPathSegment());
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

  /**
   * Get a human-readable description for the data this question collects.
   *
   * <p>NOTE: This field will not be localized as it is for admin use only.
   */
  public String getDescription() {
    return this.description;
  }

  public LocalizedStrings getQuestionText() {
    return questionText;
  }

  public LocalizedStrings getQuestionHelpText() {
    return questionHelpText;
  }

  /**
   * Get a set of {@link Locale}s that this question supports. A question fully supports a locale if
   * it provides translations for all applicant-visible text in that locale.
   */
  public ImmutableSet<Locale> getSupportedLocales() {
    // Question help text is optional
    if (questionHelpText.isEmpty()) {
      return questionText.locales();
    } else {
      return ImmutableSet.copyOf(
          Sets.intersection(questionText.locales(), questionHelpText.locales()));
    }
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
    if (questionText.hasEmptyTranslation()) {
      errors.add(CiviFormError.of("Question text cannot be blank"));
    }
    if (getQuestionType().equals(QuestionType.ENUMERATOR)) {
      EnumeratorQuestionDefinition enumeratorQuestionDefinition =
          (EnumeratorQuestionDefinition) this;
      if (enumeratorQuestionDefinition.getEntityType().hasEmptyTranslation()) {
        errors.add(CiviFormError.of("Enumerator question must have specified entity type"));
      }
    }
    if (isRepeated() && !questionTextAndHelpTextContainsRepeatedEntityNameFormatString()) {
      errors.add(
          CiviFormError.of(
              "Repeated questions must reference '$this' in the text and help text (if present)"));
    }
    if (getQuestionType().isMultiOptionType()) {
      MultiOptionQuestionDefinition multiOptionQuestionDefinition =
          (MultiOptionQuestionDefinition) this;
      if (multiOptionQuestionDefinition.getOptions().isEmpty()) {
        errors.add(CiviFormError.of("Multi-option questions must have at least one option"));
      }
      if (multiOptionQuestionDefinition.getOptions().stream()
          .anyMatch(option -> option.optionText().hasEmptyTranslation())) {
        errors.add(CiviFormError.of("Multi-option questions cannot have blank options"));
      }
      int numUniqueOptionDefaultValues =
          multiOptionQuestionDefinition.getOptions().stream()
              .map(QuestionOption::optionText)
              .map(LocalizedStrings::getDefault)
              .distinct()
              .mapToInt(s -> 1)
              .sum();
      if (numUniqueOptionDefaultValues != multiOptionQuestionDefinition.getOptions().size()) {
        errors.add(CiviFormError.of("Multi-option question options must be unique"));
      }
    }
    return errors.build();
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
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
          && this.description.equals(o.getDescription())
          && this.questionText.equals(o.getQuestionText())
          && this.questionHelpText.equals(o.getQuestionHelpText())
          && this.validationPredicates.equals(o.getValidationPredicates());
    }
    return false;
  }

  private boolean questionTextAndHelpTextContainsRepeatedEntityNameFormatString() {
    boolean textMissingFormatString =
        questionText.translations().values().stream().anyMatch(text -> !text.contains("$this"));
    boolean helpTextMissingFormatString =
        questionHelpText.translations().values().stream()
            .anyMatch(helpText -> !helpText.contains("$this"));
    return !textMissingFormatString && !helpTextMissingFormatString;
  }
}
