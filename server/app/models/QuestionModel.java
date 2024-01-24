package models;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import io.ebean.annotation.DbArray;
import io.ebean.annotation.DbJsonB;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import play.data.validation.Constraints;
import services.LocalizedStrings;
import services.question.PrimaryApplicantInfoTag;
import services.question.QuestionOption;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

/**
 * An EBean mapped class that stores the configuration for a single question.
 *
 * <p>Questions can be shared by multiple {@code Program}s. The set of questions for a given version
 * define the data model for a given applicant i.e. all of the data that can be collected for an
 * {@code Applicant} across all programs.
 *
 * <p>After they are published questions are immutable. To update a question the CiviForm admin must
 * create a new {@code Version} draft, make updates, and publish which will create a new version of
 * all updated questions.
 *
 * <p>Questions that aren't updated between versions are associated with multiple versions. I.e.
 * Questions that are not updated will be carried over to new versions.
 */
@Entity
@Table(name = "questions")
public class QuestionModel extends BaseModel {

  private QuestionDefinition questionDefinition;

  /** Different versions of the same question are linked by their immutable name. */
  private @Constraints.Required String name;

  private Long enumeratorId;

  private @Constraints.Required String description;

  /**
   * legacyQuestionText is the legacy storage column for question text translations. Questions
   * created before early May 2021 may use this, but all other question should not.
   */
  private @DbJsonB ImmutableMap<Locale, String> legacyQuestionText;

  /** questionText is the current storage column for question text translations. */
  private @DbJsonB LocalizedStrings questionText;

  /**
   * legacyQuestionHelpText is the legacy storage column for question help text translations.
   * Questions created before early May 2021 may use this, but all other question should not.
   */
  private @DbJsonB ImmutableMap<Locale, String> legacyQuestionHelpText;

  /** questionHelpText is the current storage column for question help text translations. */
  private @DbJsonB LocalizedStrings questionHelpText;

  private @Constraints.Required String questionType;

  private @Constraints.Required @DbJsonB String validationPredicates;

  // legacyQuestionOptions is the legacy storage column for multi-option questions.
  // A few questions created early on in April 2021 may use this, but all
  // other multi-option questions should not. In practice one can assume only
  // a single locale is present for questions that have values stored in this
  // column.
  private @DbJsonB ImmutableListMultimap<Locale, String> legacyQuestionOptions;

  // questionOptions is the current storage column for multi-option questions.
  private @DbJsonB ImmutableList<QuestionOption> questionOptions;

  private @DbJsonB LocalizedStrings enumeratorEntityType;

  private @DbArray List<QuestionTag> questionTags;

  /** When the question was created. */
  @WhenCreated private Instant createTime;

  /** When the question was last modified. */
  @WhenModified private Instant lastModifiedTime;

  @ManyToMany(mappedBy = "questions")
  @JoinTable(
      name = "versions_questions",
      joinColumns = @JoinColumn(name = "questions_id"),
      inverseJoinColumns = @JoinColumn(name = "versions_id"))
  private List<VersionModel> versions;

  public QuestionModel(QuestionDefinition questionDefinition) {
    this.questionDefinition = checkNotNull(questionDefinition);
    setFieldsFromQuestionDefinition(questionDefinition);
  }

  public ImmutableList<VersionModel> getVersions() {
    return ImmutableList.copyOf(versions);
  }

  public QuestionModel addVersion(VersionModel version) {
    versions.add(version);
    return this;
  }

  /** Populates column values from {@link QuestionDefinition}. */
  @PreUpdate
  @PrePersist
  public void persistChangesToQuestionDefinition() {
    setFieldsFromQuestionDefinition(questionDefinition);
  }

  /** Populates {@link QuestionDefinition} from column values. */
  @PostLoad
  @PostPersist
  @PostUpdate
  public void loadQuestionDefinition()
      throws UnsupportedQuestionTypeException, InvalidQuestionTypeException {
    // Migrate REPEATER to ENUMERATOR
    if (questionType.equalsIgnoreCase("REPEATER")) {
      questionType = "ENUMERATOR";
    }
    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setId(id)
            .setName(name)
            .setEnumeratorId(Optional.ofNullable(enumeratorId))
            .setDescription(description)
            .setQuestionType(QuestionType.valueOf(questionType))
            .setValidationPredicatesString(validationPredicates)
            .setLastModifiedTime(Optional.ofNullable(lastModifiedTime))
            .setUniversal(questionTags.contains(QuestionTag.UNIVERSAL))
            .setPrimaryApplicantInfoTags(getPrimaryApplicantInfoTagsFromQuestionTags(questionTags));

    setEnumeratorEntityType(builder);

    // Build accounting for legacy columns
    setQuestionText(builder);
    setQuestionHelpText(builder);
    setQuestionOptions(builder);

    this.questionDefinition = builder.build();
  }

  /**
   * Given a list of {@link QuestionTag}s, fetch the {@link PrimaryApplicantInfoTag}s that
   * correspond to those QuestionTags. Any {@link QuestionTag} that doesn't have a corresponding
   * {@link PrimaryApplicantInfoTag} is ignored.
   *
   * @param questionTags The list of {@link QuestionTag}s to check.
   * @return An {@link ImmutableSet} of {@link PrimaryApplicantInfoTag} corresponding to the given
   *     {@link QuestionTag}s.
   */
  private ImmutableSet<PrimaryApplicantInfoTag> getPrimaryApplicantInfoTagsFromQuestionTags(
      List<QuestionTag> questionTags) {
    return ImmutableList.copyOf(PrimaryApplicantInfoTag.values()).stream()
        .filter(
            primaryApplicantInfoTag ->
                questionTags.contains(primaryApplicantInfoTag.getQuestionTag()))
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Add {@link LocalizedStrings} for question text to the builder, taking into account legacy
   * columns.
   *
   * <p>The majority of questions should have `questionText` and not `legacyQuestionText`.
   */
  private QuestionModel setQuestionText(QuestionDefinitionBuilder builder) {
    LocalizedStrings textToSet =
        Optional.ofNullable(questionText)
            .orElseGet(() -> LocalizedStrings.create(legacyQuestionText));
    builder.setQuestionText(textToSet);
    return this;
  }

  /**
   * Add {@link LocalizedStrings} for question help text to the builder, taking into account legacy
   * columns.
   *
   * <p>The majority of questions should have `questionHelpText` and not `legacyQuestionHelpText`.
   */
  private QuestionModel setQuestionHelpText(QuestionDefinitionBuilder builder) {
    LocalizedStrings textToSet =
        Optional.ofNullable(questionHelpText)
            .orElseGet(
                () -> LocalizedStrings.create(legacyQuestionHelpText, /* canBeEmpty= */ true));
    builder.setQuestionHelpText(textToSet);
    return this;
  }

  /**
   * Add {@link QuestionOption}s to the builder, taking into account legacy columns.
   *
   * <p>The majority of questions should have a `questionOptions` and not `legacyQuestionOptions`.
   */
  private QuestionModel setQuestionOptions(QuestionDefinitionBuilder builder)
      throws InvalidQuestionTypeException {
    if (!QuestionType.of(questionType).isMultiOptionType()) {
      return this;
    }

    // The majority of multi option questions should have `questionOptions` and not
    // `legacyQuestionOptions`.
    // `legacyQuestionOptions` is a legacy implementation that only supported a single locale.
    if (questionOptions != null) {
      builder.setQuestionOptions(questionOptions);
      return this;
    }

    // If the multi option question does have legacyQuestionOptions, we can assume there is only one
    // locale and convert the strings to QuestionOption instances each with a single locale.
    Locale firstKey = legacyQuestionOptions.keySet().stream().iterator().next();

    ImmutableList<QuestionOption> options =
        Streams.mapWithIndex(
                legacyQuestionOptions.get(firstKey).stream(),
                (optionText, i) ->
                    QuestionOption.create(
                        Long.valueOf(i),
                        Long.valueOf(i),
                        optionText,
                        LocalizedStrings.of(firstKey, optionText)))
            .collect(toImmutableList());

    builder.setQuestionOptions(options);
    return this;
  }

  private QuestionModel setEnumeratorEntityType(QuestionDefinitionBuilder builder)
      throws InvalidQuestionTypeException {
    if (QuestionType.of(questionType).equals(QuestionType.ENUMERATOR)) {
      builder.setEntityType(enumeratorEntityType);
    }
    return this;
  }

  public QuestionDefinition getQuestionDefinition() {
    return checkNotNull(questionDefinition);
  }

  private QuestionModel setFieldsFromQuestionDefinition(QuestionDefinition questionDefinition) {
    if (questionDefinition.isPersisted()) {
      id = questionDefinition.getId();
    }
    enumeratorId = questionDefinition.getEnumeratorId().orElse(null);
    name = questionDefinition.getName();
    description = questionDefinition.getDescription();
    questionText = questionDefinition.getQuestionText();
    questionHelpText = questionDefinition.getQuestionHelpText();
    questionType = questionDefinition.getQuestionType().toString();
    validationPredicates = questionDefinition.getValidationPredicatesAsString();

    if (questionDefinition.getQuestionType().isMultiOptionType()) {
      MultiOptionQuestionDefinition multiOption =
          (MultiOptionQuestionDefinition) questionDefinition;
      questionOptions = multiOption.getOptions();
    }

    if (questionDefinition.getQuestionType().equals(QuestionType.ENUMERATOR)) {
      EnumeratorQuestionDefinition enumerator = (EnumeratorQuestionDefinition) questionDefinition;
      enumeratorEntityType = enumerator.getEntityType();
    }

    // We must ensure we always initTags here. Otherwise, if we aren't
    // adding the tag, and we're needing to remove the universal tag
    // from an existing question, we'd end up with the questionTags field
    // being null, which will simply not update the tags in the database at all.
    if (questionDefinition.isUniversal()) {
      addTag(QuestionTag.UNIVERSAL);
    } else {
      initTags();
    }

    // Add QuestionTags for PrimaryApplicantInfoTags in the list. Note that this must come after
    // we've
    // done initTags above, either in this function or in addTag previously.
    questionDefinition
        .getPrimaryApplicantInfoTags()
        .forEach(primaryApplicantInfoTag -> addTag(primaryApplicantInfoTag.getQuestionTag()));

    return this;
  }

  public boolean removeVersion(VersionModel draftVersion) {
    return this.versions.remove(draftVersion);
  }

  private void initTags() {
    if (this.questionTags == null) {
      this.questionTags = new ArrayList<>();
    }
  }

  /** Adds the specified tag, returning true if it was not already present. */
  public boolean addTag(QuestionTag tag) {
    initTags();
    if (this.questionTags.contains(tag)) {
      return false;
    }
    this.questionTags.add(tag);
    return true;
  }

  /** Remove the specified tag, returning true if it was present. */
  public boolean removeTag(QuestionTag tag) {
    initTags();
    return this.questionTags.remove(tag);
  }

  /** Return true if the tag is present. */
  public boolean containsTag(QuestionTag tag) {
    initTags();
    return this.questionTags.contains(tag);
  }

  /** Return all the tags on this question. */
  public ImmutableList<QuestionTag> getQuestionTags() {
    initTags();
    return ImmutableList.copyOf(this.questionTags);
  }

  public Optional<Instant> getCreateTime() {
    return Optional.ofNullable(createTime);
  }
}
