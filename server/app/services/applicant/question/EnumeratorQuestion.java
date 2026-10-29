package services.applicant.question;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import forms.questions.EnumeratorQuestionForm;
import java.util.OptionalInt;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.EnumeratorQuestionDefinition;

/**
 * Represents an enumerator question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class EnumeratorQuestion extends AbstractQuestion {

  private final boolean initialQuestionFlowIsActive;

  EnumeratorQuestion(ApplicantQuestion applicantQuestion) {
    this(applicantQuestion, /* initialQuestionFlowIsActive= */ false);
  }

  EnumeratorQuestion(ApplicantQuestion applicantQuestion, boolean initialQuestionFlowIsActive) {
    super(applicantQuestion);
    this.initialQuestionFlowIsActive = initialQuestionFlowIsActive;
  }

  @Override
  public boolean isAnswered() {
    // This is answered if the the path to the enumerator question answer array exists.
    return applicantQuestion
        .getApplicantData()
        .hasPath(applicantQuestion.getContextualizedPath().atIndex(0));
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    // Not intended to return the leaf question paths.
    return ImmutableList.of();
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    // No blank values are allowed. No duplicated entity names are allowed.
    return ImmutableMap.of(applicantQuestion.getContextualizedPath(), validateEntities());
  }

  private ImmutableSet<ValidationErrorMessage> validateEntities() {
    ImmutableSet.Builder<ValidationErrorMessage> errorsBuilder = ImmutableSet.builder();
    ImmutableList<String> entityNames = getEntityNames();

    // When the initial-question flow produced these entities, the initial question's own per-entity
    // validation
    // already reports a missing answer. Skip the enumerator-level blank-name check and ignore blank
    // (unanswered) names when checking for duplicates so the applicant does not see a redundant
    // top-level error. This is gated on the runtime flow being active (not just an initial question
    // being configured) so a legacy free-text submission still validates blank names normally.
    ImmutableList<String> namesToValidate =
        initialQuestionFlowIsActive
            ? entityNames.stream()
                .filter(name -> !name.isBlank())
                .collect(ImmutableList.toImmutableList())
            : entityNames;

    if (namesToValidate.stream().anyMatch(String::isBlank)) {
      errorsBuilder.add(
          ValidationErrorMessage.create(MessageKey.ENUMERATOR_VALIDATION_ENTITY_REQUIRED));
    }
    if (namesToValidate.stream().collect(ImmutableSet.toImmutableSet()).size()
        != namesToValidate.size()) {
      errorsBuilder.add(
          ValidationErrorMessage.create(MessageKey.ENUMERATOR_VALIDATION_DUPLICATE_ENTITY_NAME));
    }
    if (getMinEntities().isPresent() && entityNames.size() < getMinEntities().getAsInt()) {
      errorsBuilder.add(
          ValidationErrorMessage.create(
              MessageKey.ENUMERATOR_VALIDATION_TOO_FEW_ENTITIES, getMinEntities().getAsInt()));
    }
    int maxAllowed = getMaxEntities().orElse(EnumeratorQuestionForm.MAX_ENUM_ENTITIES_ALLOWED);
    if (entityNames.size() > maxAllowed) {
      errorsBuilder.add(
          ValidationErrorMessage.create(
              MessageKey.ENUMERATOR_VALIDATION_TOO_MANY_ENTITIES, maxAllowed));
    }

    return errorsBuilder.build();
  }

  public EnumeratorQuestionDefinition getQuestionDefinition() {
    return (EnumeratorQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  /** Return the repeated entity names associated with this enumerator question. */
  public ImmutableList<String> getEntityNames() {
    return applicantQuestion
        .getApplicantData()
        .readRepeatedEntities(applicantQuestion.getContextualizedPath());
  }

  /**
   * Get the localized admin-configurable entity type this enumerator represents. Examples: "car",
   * "child", "job", "household member".
   */
  public String getEntityType() {
    return getQuestionDefinition()
        .getEntityType()
        .getOrDefault(applicantQuestion.getApplicantData().preferredLocale());
  }

  public OptionalInt getMinEntities() {
    return getQuestionDefinition().getMinEntities();
  }

  public OptionalInt getMaxEntities() {
    return getQuestionDefinition().getMaxEntities();
  }

  @Override
  public String getAnswerString() {
    return Joiner.on("\n").join(getEntityNames());
  }
}
