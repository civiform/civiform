package services.applicant.question;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
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
    if (!validQuestionTypes().contains(applicantQuestion.getType())) {
      throw new RuntimeException(
          String.format(
              "Question is not a question of the following types: [%s]: %s (type: %s)",
              Joiner.on(", ").join(validQuestionTypes().stream().toArray()),
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  /**
   * The set of acceptable question types for the {@link ApplicantQuestion} provided in the
   * constructor. This is used for validation purposes.
   */
  protected abstract ImmutableSet<QuestionType> validQuestionTypes();

  /**
   * Returns any {@link ValidationErrorMessage}s to be shown to the applicant, keyed by the relevant
   * field path. Top-level question errors use the root question path.
   */
  public final ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrors() {
    if (!isAnswered()) {
      return ImmutableMap.of();
    }
    // Why not just return the result of getValidationErrorsInternal()?
    // For ease of implementation, subclasses may build the error list by putting a field key
    // in the map along with a call to a validator method that may return an empty set of errors.
    // We remove keys with an empty set of errors here to help defend against downstream consumers
    // assumes that calling isEmpty on the map means that there are no errors.
    return ImmutableMap.<Path, ImmutableSet<ValidationErrorMessage>>builder()
        .putAll(Maps.filterEntries(getValidationErrorsInternal(), e -> !e.getValue().isEmpty()))
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
    return getAllPaths().stream().anyMatch(p -> applicantQuestion.getApplicantData().hasPath(p));
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
}