package services.question.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import services.CiviFormError;
import services.LocalizedStrings;
import services.Path;
import services.applicant.RepeatedEntity;
import services.applicant.question.Scalar;
import services.export.enums.ApiPathSegment;
import services.question.PrimaryApplicantInfoTag;
import services.question.QuestionOption;

/**
 * Superclass for all question types.
 *
 * <p>The {@link JsonSubTypes} information lets us parse a QuestionDefinition into JSON, and then
 * deserialize the JSON back into the correct QuestionDefinition subclass.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AddressQuestionDefinition.class, name = "address"),
  @JsonSubTypes.Type(value = CurrencyQuestionDefinition.class, name = "currency"),
  @JsonSubTypes.Type(value = DateQuestionDefinition.class, name = "date"),
  @JsonSubTypes.Type(value = EmailQuestionDefinition.class, name = "email"),
  @JsonSubTypes.Type(value = EnumeratorQuestionDefinition.class, name = "enumerator"),
  @JsonSubTypes.Type(value = FileUploadQuestionDefinition.class, name = "fileupload"),
  @JsonSubTypes.Type(value = IdQuestionDefinition.class, name = "id"),
  @JsonSubTypes.Type(value = MultiOptionQuestionDefinition.class, name = "multioption"),
  @JsonSubTypes.Type(value = NameQuestionDefinition.class, name = "name"),
  @JsonSubTypes.Type(value = NumberQuestionDefinition.class, name = "number"),
  @JsonSubTypes.Type(value = PhoneQuestionDefinition.class, name = "phone"),
  @JsonSubTypes.Type(value = StaticContentQuestionDefinition.class, name = "static"),
  @JsonSubTypes.Type(value = TextQuestionDefinition.class, name = "text"),
})
public abstract class QuestionDefinition {

  @JsonProperty("config")
  private QuestionDefinitionConfig config;

  protected QuestionDefinition(QuestionDefinitionConfig config) {
    if (config.validationPredicates().isEmpty()) {
      config = config.toBuilder().setValidationPredicates(getDefaultValidationPredicates()).build();
    }

    this.config = config;
  }

  public abstract static class ValidationPredicates {
    protected static final ObjectMapper mapper =
        new ObjectMapper().registerModule(new GuavaModule()).registerModule(new Jdk8Module());

    static {
      mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    public String serializeAsString() {
      try {
        return mapper.writeValueAsString(this);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  // Note: All the methods below are @JsonIgnore-d because they all pull information from the {@code
  // config} object, and the config object is already JSON-serialized. Methods that are not
  // @JsonIgnore-d are not automatically serialized by Jackson because they are not getters (do not
  // start with "is" or "get") and do not represent a field on the object.

  /** Return true if the question is persisted and has an unique identifier. */
  @JsonIgnore
  public final boolean isPersisted() {
    return config.id().isPresent();
  }

  /** Get the unique identifier for this question. */
  @JsonIgnore
  public final long getId() {
    return config.id().getAsLong();
  }

  /** True if the question is marked as a universal question. */
  @JsonIgnore
  public final boolean isUniversal() {
    return config.universal();
  }

  @JsonIgnore
  public final ImmutableSet<PrimaryApplicantInfoTag> getPrimaryApplicantInfoTags() {
    return config.primaryApplicantInfoTags();
  }

  /** True if this QuestionDefinition contains the given PrimaryApplicantInfoTag. */
  public final boolean containsPrimaryApplicantInfoTag(
      PrimaryApplicantInfoTag primaryApplicantInfoTag) {
    return config.primaryApplicantInfoTags().contains(primaryApplicantInfoTag);
  }

  /**
   * Get the name of this question.
   *
   * <p>Different versions of the same program are linked by their immutable name.
   *
   * <p>NOTE: This field will not be localized as it is for admin use only.
   */
  @JsonIgnore
  public final String getName() {
    return config.name();
  }

  @JsonIgnore
  public final Optional<Instant> getLastModifiedTime() {
    return config.lastModifiedTime();
  }

  // Note that this formatting logic is duplicated in main.ts formatQuestionName()
  @JsonIgnore
  public final String getQuestionNameKey() {
    return config.name().replaceAll("[^a-zA-Z ]", "").replaceAll("\\s", "_");
  }

  /** Returns the {@link Path} segment that corresponds to this QuestionDefinition. */
  @JsonIgnore
  public final String getQuestionPathSegment() {
    // TODO(#783): Change this getter once we save this formatted name to the database.
    String formattedName = getQuestionNameKey();
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
  public final Path getContextualizedPath(
      Optional<RepeatedEntity> repeatedEntity, Path defaultRoot) {
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
  @JsonIgnore
  public final boolean isEnumerator() {
    return getQuestionType().equals(QuestionType.ENUMERATOR);
  }

  /**
   * See {@link #getEnumeratorId()}.
   *
   * @return true if this is a repeated question.
   */
  @JsonIgnore
  public final boolean isRepeated() {
    return config.enumeratorId().isPresent();
  }

  /** True if the question is an {@link AddressQuestionDefinition}. */
  @JsonIgnore
  public final boolean isAddress() {
    return getQuestionType().equals(QuestionType.ADDRESS);
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
  @JsonIgnore
  public final Optional<Long> getEnumeratorId() {
    return config.enumeratorId();
  }

  /**
   * Get a human-readable description for the data this question collects.
   *
   * <p>NOTE: This field will not be localized as it is for admin use only.
   */
  @JsonIgnore
  public final String getDescription() {
    return config.description();
  }

  @JsonIgnore
  public final LocalizedStrings getQuestionText() {
    return config.questionText();
  }

  @JsonIgnore
  public final LocalizedStrings getQuestionHelpText() {
    return config.questionHelpText();
  }

  /**
   * Get a set of {@link Locale}s that this question supports. A question fully supports a locale if
   * it provides translations for all applicant-visible text in that locale.
   */
  @JsonIgnore
  public ImmutableSet<Locale> getSupportedLocales() {
    // Question help text is optional
    if (config.questionHelpText().isEmpty()) {
      return config.questionText().locales();
    } else {
      return ImmutableSet.copyOf(
          Sets.intersection(config.questionText().locales(), getQuestionHelpText().locales()));
    }
  }

  /** Get the validation predicates. */
  @JsonIgnore
  public final ValidationPredicates getValidationPredicates() {
    return config.validationPredicates().orElseGet(this::getDefaultValidationPredicates);
  }

  /** Serialize validation predicates as a string. This is used for persisting in database. */
  @JsonIgnore
  public final String getValidationPredicatesAsString() {
    return getValidationPredicates().serializeAsString();
  }

  /** Get the type of this question. */
  @JsonIgnore
  public abstract QuestionType getQuestionType();

  /** Get the default validation predicates for this question type. */
  @JsonIgnore
  abstract ValidationPredicates getDefaultValidationPredicates();

  /**
   * Overload of {@code validate()} that sets {@code previousDefinition} to empty.
   *
   * <p>See {@link #validate(Optional)} for the implications of not providing a {@code
   * previousDefinition} when validating.
   */
  public final ImmutableSet<CiviFormError> validate() {
    return validate(Optional.empty());
  }

  /**
   * Validate that all required fields are present and valid for the question.
   *
   * <p>If {@code previousDefinition} is provided, only the admin names of the new {@link
   * QuestionOption} need to pass validation.
   *
   * @param previousDefinition the optional previous version of the {@link QuestionDefinition}
   * @throws IllegalArgumentException if the previousDefinition is not of the same type as this
   *     definition.
   */
  public final ImmutableSet<CiviFormError> validate(
      Optional<QuestionDefinition> previousDefinition) {
    if (previousDefinition.isPresent()
        && previousDefinition.get().getQuestionType() != getQuestionType()) {
      throw new IllegalArgumentException(
          "The previous version of the question definition must be of the same question type as the"
              + " updated version.");
    }

    ImmutableSet.Builder<CiviFormError> errors = new ImmutableSet.Builder<>();
    if (config.questionText().isEmpty()) {
      errors.add(CiviFormError.of("Question text cannot be blank"));
    }
    if (config.questionText().hasEmptyTranslation()) {
      errors.add(CiviFormError.of("Question text cannot be blank"));
    }
    if (config.name().isBlank()) {
      errors.add(CiviFormError.of("Administrative identifier cannot be blank"));
    }
    if (getQuestionPathSegment().equals(Path.empty().join(Scalar.ENTITY_NAME).toString())) {
      errors.add(
          CiviFormError.of(
              String.format("Administrative identifier '%s' is not allowed", getName())));
    }
    if (getQuestionPathSegment()
        .equals(Path.empty().join(ApiPathSegment.ENTITY_METADATA).toString())) {
      errors.add(
          CiviFormError.of(
              String.format("Administrative identifier '%s' is not allowed", getName())));
    }
    if (getQuestionType().equals(QuestionType.ENUMERATOR)) {
      EnumeratorQuestionDefinition enumeratorQuestionDefinition =
          (EnumeratorQuestionDefinition) this;
      if (enumeratorQuestionDefinition.getEntityType().hasEmptyTranslation()) {
        errors.add(CiviFormError.of("Enumerator question must have specified entity type"));
      }
    }

    if (isRepeated() && !questionTextContainsRepeatedEntityNameFormatString()) {
      errors.add(CiviFormError.of("Repeated questions must reference '$this' in the text"));
    }

    if (getQuestionType().isMultiOptionType()) {
      MultiOptionQuestionDefinition multiOptionQuestionDefinition =
          (MultiOptionQuestionDefinition) this;

      if (multiOptionQuestionDefinition.getOptions().isEmpty()) {
        errors.add(CiviFormError.of("Multi-option questions must have at least one option"));
      }

      if (multiOptionQuestionDefinition.getOptionAdminNames().stream().anyMatch(String::isEmpty)) {
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

      if (multiOptionQuestionDefinition.getOptionAdminNames().stream()
          // This is O(n^2) but the list is small and it's simpler than creating a Set
          .filter(n -> !existingAdminNames.contains(n.toLowerCase(Locale.ROOT)))
          .anyMatch(s -> !s.matches("[0-9a-z_-]+"))) {
        errors.add(
            CiviFormError.of(
                "Multi-option admin names can only contain lowercase letters, numbers, underscores,"
                    + " and dashes"));
      }
      if (multiOptionQuestionDefinition.getOptions().stream()
          .anyMatch(option -> option.optionText().hasEmptyTranslation())) {
        errors.add(CiviFormError.of("Multi-option questions cannot have blank options"));
      }

      int numOptions = multiOptionQuestionDefinition.getOptions().size();
      long numUniqueOptionDefaultValues =
          multiOptionQuestionDefinition.getOptions().stream()
              .map(QuestionOption::optionText)
              .map(e -> e.getDefault().toLowerCase(Locale.ROOT))
              .distinct()
              .count();
      if (numUniqueOptionDefaultValues != numOptions) {
        errors.add(CiviFormError.of("Multi-option question options must be unique"));
      }

      long numUniqueOptionAdminNames =
          multiOptionQuestionDefinition.getOptions().stream()
              .map(QuestionOption::adminName)
              .map(e -> e.toLowerCase(Locale.ROOT))
              .distinct()
              .count();
      if (numUniqueOptionAdminNames != numOptions) {
        errors.add(CiviFormError.of("Multi-option question admin names must be unique"));
      }

      OptionalInt minChoicesRequired =
          multiOptionQuestionDefinition.getMultiOptionValidationPredicates().minChoicesRequired();
      OptionalInt maxChoicesAllowed =
          multiOptionQuestionDefinition.getMultiOptionValidationPredicates().maxChoicesAllowed();
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
    }
    return errors.build();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }

  /** Two QuestionDefinitions are considered equal if all of their properties are the same. */
  @Override
  public boolean equals(Object other) {
    return this.idEquals(other) && this.equalsIgnoreId(other);
  }

  private boolean idEquals(Object other) {
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
  private boolean equalsIgnoreId(Object other) {
    if (other instanceof QuestionDefinition) {
      QuestionDefinition o = (QuestionDefinition) other;

      return getQuestionType().equals(o.getQuestionType())
          && getName().equals(o.getName())
          && getDescription().equals(o.getDescription())
          && getQuestionText().equals(o.getQuestionText())
          && getQuestionHelpText().equals(o.getQuestionHelpText())
          && getValidationPredicates().equals(o.getValidationPredicates());
    }
    return false;
  }

  private boolean questionTextContainsRepeatedEntityNameFormatString() {
    return getQuestionText().translations().values().stream()
        .allMatch(text -> text.contains("$this"));
  }

  /**
   * TODO(#5271): remove this. This is only used for {@link QuestionDefinitionBuilder} in order to
   * construct new instances, and {@link QuestionDefinitionBuilder} should be removed.
   *
   * <p>The {@link QuestionDefinitionConfig} should be entirely internal to {@link
   * QuestionDefinition}.
   */
  QuestionDefinitionConfig getConfig() {
    return config;
  }

  /**
   * Tests that use {@link QuestionDefinition} are required to have an ID in the question at some
   * points, but usually it's not populated until it's inserted in the DB. This method populates the
   * ID for testing purposes only.
   */
  @VisibleForTesting
  public QuestionDefinition withPopulatedTestId() {
    config = config.toBuilder().setId(new Random().nextLong()).build();
    return this;
  }
}
