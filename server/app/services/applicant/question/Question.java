package services.applicant.question;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import models.ApplicantModel;

import java.util.HashMap;
import services.MessageKey;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.PrimaryApplicantInfoTag;
import services.question.types.QuestionType;
import java.util.Objects;

/**
 * All specific applicant question types extend this class.
 *
 * <p>It provides necessary methods to:
 *
 * <ul>
 *   <li>Access question configuration
 *   <li>Access the applicant's response
 *   <li>Validate the applicant's answer to the question: Conditions and data integrity
 *   <li>Present validation errors if any
 * </ul>
 */
public abstract class Question {
  protected final ApplicantQuestion applicantQuestion;

  public Question(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = Preconditions.checkNotNull(applicantQuestion);

    QuestionType supportedQuestionType = applicantQuestion.getType();
    if (!getClass().equals(supportedQuestionType.getSupportedQuestion())) {
      throw new RuntimeException(
          String.format(
              "The Question class %s is not equal to the one supported by %s, which is %s.",
              getClass(),
              supportedQuestionType,
              supportedQuestionType.getSupportedQuestion().toString()));
    }
  }

  /**
   * Returns any {@link ValidationErrorMessage}s to be shown to the applicant, keyed by the relevant
   * field path. Top-level question errors use the root question path.
   */
  public final ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrors() {
    ImmutableMap<Path, String> failedUpdates =
        applicantQuestion.getApplicantData().getFailedUpdates();
    if (!isAnswered() && applicantQuestion.isOptional() && failedUpdates.isEmpty()) {
      return ImmutableMap.of();
    }

    HashMap<Path, ImmutableSet<ValidationErrorMessage>> errorMap = new HashMap<>();

    // Why not just use the result of getValidationErrorsInternal()?
    // For ease of implementation, subclasses may build the error list by putting a field key
    // in the map along with a call to a validator method that may return an empty set of errors.
    // We remove keys with an empty set of errors here to help defend against downstream consumers
    // assumes that calling isEmpty on the map means that there are no errors.
    errorMap.putAll(
        Maps.filterEntries(getValidationErrorsInternal(), e -> !e.getValue().isEmpty()));

    // We shouldn't have an empty error map if we failed to convert some of the input. If there
    // aren't already errors, append a top-level error that the input couldn't be converted. In
    // practice, this shouldn't happen as long as each question type is properly accounting for bad
    // input.
    if (errorMap.isEmpty()
        && !failedUpdates.isEmpty()
        && getAllPaths().stream().anyMatch(failedUpdates::containsKey)) {
      errorMap.put(
          applicantQuestion.getContextualizedPath(),
          ImmutableSet.of(ValidationErrorMessage.create(MessageKey.INVALID_INPUT)));
    }

    // Add error for unanswered required question.
    if (applicantQuestion.isRequiredButWasSkippedInCurrentProgram()) {
      Path keyPath = applicantQuestion.getContextualizedPath();
      var valueErrors = new ImmutableSet.Builder<ValidationErrorMessage>();
      if (errorMap.containsKey(keyPath)) {
        valueErrors.addAll(errorMap.get(keyPath));
      }
      valueErrors.add(ValidationErrorMessage.create(MessageKey.VALIDATION_REQUIRED));
      errorMap.put(keyPath, valueErrors.build());
    }

    return new ImmutableMap.Builder<Path, ImmutableSet<ValidationErrorMessage>>()
        .putAll(errorMap)
        .build();
  }

  /**
   * Question-type specific implementation of {@link Question.getValidationErrors}. Note that keys
   * with an empty set of errors will be filtered out by {@link Question.getValidationErrors} so
   * that calls to isEmpty on the getvalidationErrors result are sufficient to indicate if there any
   * errors.
   */
  protected abstract ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>>
      getValidationErrorsInternal();

  /**
   * A question is considered answered if the applicant data has been set for any of the paths
   * associated with the question or if the question is tagged with a Primary Applicant Info
   * Tag and the applicant has data saved in the corresponding column.
   */
  public boolean isAnswered() {
    boolean isAnsweredWithApplicantData = getAllPaths().stream().anyMatch(applicantQuestion.getApplicantData()::hasPath);

    // Filter to see if the applicant has data saved for any of the PAI tags on the question definition.
    // If the returned set is NOT empty, then the applicant has data saved and the question is "answered"
    ImmutableSet<PrimaryApplicantInfoTag> tags = applicantQuestion.getQuestionDefinition().getPrimaryApplicantInfoTags();
    boolean isAnsweredWithPrimaryApplicantInfo = !tags.stream().filter(tag -> {
      ApplicantModel applicant = applicantQuestion.getApplicantData().getApplicant();
      switch (tag) {
        case APPLICANT_EMAIL:
          return applicant.getEmailAddress().isPresent();
        case APPLICANT_PHONE:
          return applicant.getPhoneNumber().isPresent();
        case APPLICANT_DOB:
          return applicant.getDateOfBirth().isPresent();
        case APPLICANT_NAME:
          return applicant.getFirstName().isPresent();
        default:
          return false;
      }
    }).collect(ImmutableSet.toImmutableSet()).isEmpty();

    return isAnsweredWithApplicantData || isAnsweredWithPrimaryApplicantInfo;
  }

  /**
   * Returns the answer as a text string.
   *
   * <p>This is the canonical representation to users in static contexts such as the review page and
   * data export.
   */
  public abstract String getAnswerString();

  /** Return every path used by this question. */
  public abstract ImmutableList<Path> getAllPaths();

  public final ImmutableMap<Path, String> getFailedUpdates() {
    return applicantQuestion.getApplicantData().getFailedUpdates();
  }

  public ApplicantQuestion getApplicantQuestion() {
    return applicantQuestion;
  }
}
