package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import services.Path;
import services.applicant.ValidationErrorMessage;

/**
 * All specific applicant question types implement this interface.
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
public interface Question {
  /**
   * Returns any {@link ValidationErrorMessage}s to be shown to the applicant, keyed by the relevant
   * field path. Top-level question errors use the root question path.
   */
  ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrors();

  /**
   * Returns true any part of the question has been answered by the applicant. Blank answers should
   * not count. If a question is not answered, it should not have errors associated with it.
   */
  boolean isAnswered();

  /**
   * Returns the answer as a text string.
   *
   * <p>This is the canonical representation to users in static contexts such as the review page and
   * data export.
   */
  String getAnswerString();

  /** Return every path used by this question. */
  ImmutableList<Path> getAllPaths();
}
