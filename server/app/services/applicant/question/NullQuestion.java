package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import services.Path;
import services.applicant.ValidationErrorMessage;

/**
 * This represents a missing questions and acts as a placeholder. This question does not get
 * surfaced to the user or stored in the database
 *
 * <p>In normal operation this shouldn't ever be reached, but an occasional gremlin has resulted in
 * the rare instance of a program pointing at an old version of a question.
 */
public class NullQuestion extends AbstractQuestion {
  NullQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  /**
   * Question-type specific implementation of {@link AbstractQuestion}. Note that keys with an empty
   * set of errors will be filtered out by {@link AbstractQuestion} so that calls to isEmpty on the
   * getvalidationErrors result are sufficient to indicate if there any errors.
   */
  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    return ImmutableMap.of();
  }

  /**
   * Returns the answer as a text string.
   *
   * <p>This is the canonical representation to users in static contexts such as the review page and
   * data export.
   */
  @Override
  public String getAnswerString() {
    return "";
  }

  /** Return every path used by this question. */
  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of();
  }
}
