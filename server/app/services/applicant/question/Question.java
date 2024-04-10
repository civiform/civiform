package services.applicant.question;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.HashMap;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.QuestionType;

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
   * associated with the question. If the applicant data does not contain the question's path, then
   * it will be considered unanswered.
   */
  public boolean isAnswered() {
    return getAllPaths().stream().anyMatch(applicantQuestion.getApplicantData()::hasPath);
  }

  /**
   * Returns the answer as a text string.
   *
   * <p>This is the canonical representation to users in static contexts such as the review page and
   * data export.
   */
  public abstract String getAnswerString();

  /** Returns the default to use when there is no answer */
  public String getDefaultAnswerString() {
    return "-";
  }

  /** Return every path used by this question. */
  public abstract ImmutableList<Path> getAllPaths();

  public Path getFirstPathWithError() {
    ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors =
        getValidationErrors();
    for (Path path : getAllPaths()) {
      if (validationErrors.containsKey(path)) {
        return path;
      }
    }
    return null;
  }

  public final ImmutableMap<Path, String> getFailedUpdates() {
    return applicantQuestion.getApplicantData().getFailedUpdates();
  }

  public ApplicantQuestion getApplicantQuestion() {
    return applicantQuestion;
  }
}
