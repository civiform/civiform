package services.applicant.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import forms.questions.EnumeratorQuestionForm;
import java.util.Optional;
import java.util.OptionalInt;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionType;

/**
 * Represents an enumerator question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class EnumeratorQuestion extends AbstractQuestion {

  // The initial question's type when the list set uses the initial-question flow, or empty for the
  // legacy free-text entry. It picks the duplicate-entry message.
  // TODO(#11943): Part of the initialQuestionFlowIsActive signal. When removing the blank-name
  // check in validateEntities(), pass the initial question's type whenever the list set has one.
  private final Optional<QuestionType> initialQuestionType;

  EnumeratorQuestion(ApplicantQuestion applicantQuestion) {
    this(applicantQuestion, /* initialQuestionType= */ Optional.empty());
  }

  EnumeratorQuestion(
      ApplicantQuestion applicantQuestion, Optional<QuestionType> initialQuestionType) {
    super(applicantQuestion);
    this.initialQuestionType = checkNotNull(initialQuestionType);
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

  /**
   * Validates the enumerator's entity names: no blank names, no duplicate names, and within any
   * configured minimum/maximum entity counts.
   *
   * <p>TODO(#11943): Once the enumerator feature flag is removed and every enumerator has an
   * initial question, delete the blank-name check below and remove the initialQuestionFlowIsActive
   * signal that gates it. The duplicate check still needs the initial question's type. That signal
   * is threaded through several files, each marked with a pointer back to this TODO:
   *
   * <ul>
   *   <li>the field and 2-arg constructor here;
   *   <li>ApplicantQuestion#createEnumeratorQuestion(Optional);
   *   <li>Block#initialQuestionFlowIsActive;
   *   <li>the context variable in ApplicantProgramBlockEditView;
   *   <li>the argument in EnumeratorQuestionFragment.html.
   * </ul>
   *
   * <p>Keep the duplicate-entity check, but make its blank-name filter below unconditional.
   */
  private ImmutableSet<ValidationErrorMessage> validateEntities() {
    ImmutableSet.Builder<ValidationErrorMessage> errorsBuilder = ImmutableSet.builder();
    ImmutableList<String> entityNames = getEntityNames();

    // When the initial-question flow is active, the initial question's own per-entity
    // validation already reports a missing answer. Skip the enumerator-level blank-name check
    // and ignore blank (unanswered) names when checking for duplicates so the applicant
    // does not see a redundant top-level error. This is gated on the new enumerator flow
    // being enabled (not just an initial question being configured) so a legacy free-text
    // submission still validates blank names normally.
    ImmutableList<String> entitiesToValidate =
        initialQuestionType.isPresent()
            ? entityNames.stream()
                .filter(name -> !name.isBlank())
                .collect(ImmutableList.toImmutableList())
            : entityNames;

    if (entitiesToValidate.stream().anyMatch(String::isBlank)) {
      errorsBuilder.add(
          ValidationErrorMessage.create(MessageKey.ENUMERATOR_VALIDATION_ENTITY_REQUIRED));
    }
    if (entitiesToValidate.stream().collect(ImmutableSet.toImmutableSet()).size()
        != entitiesToValidate.size()) {
      errorsBuilder.add(ValidationErrorMessage.create(duplicateEntityMessageKey()));
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

  /**
   * The duplicate error names the problem the way the applicant answered: a typed entry, or an
   * option picked from a list. The legacy free-text entry keeps its original message.
   */
  private MessageKey duplicateEntityMessageKey() {
    return initialQuestionType
        .map(
            type ->
                type.isMultiOptionType()
                    ? MessageKey.ENUMERATOR_VALIDATION_DUPLICATE_OPTION
                    : MessageKey.ENUMERATOR_VALIDATION_DUPLICATE_ENTRY)
        .orElse(MessageKey.ENUMERATOR_VALIDATION_DUPLICATE_ENTITY_NAME);
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
