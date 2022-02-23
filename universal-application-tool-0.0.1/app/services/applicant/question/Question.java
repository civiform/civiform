package services.applicant.question;

import com.google.common.collect.ImmutableList;
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
  /** Returns true if values do not meet conditions defined by admins. */
  boolean hasConditionErrors();

  /** Returns a set of {@link ValidationErrorMessage}s related to conditions defined by admins. */
  ImmutableSet<ValidationErrorMessage> getQuestionErrors();

  /**
   * Returns true if there is any type specific errors. The validation does not consider
   * admin-defined conditions.
   */
  boolean hasTypeSpecificErrors();

  /**
   * Returns a set of {@link ValidationErrorMessage}s to be shown to the applicant. These errors are
   * inherent to the question type itself - for example, an applicant providing a zip code with
   * letters would be a type error for an address question.
   */
  ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors();

  /**
   * Returns true any part of the question has been answered by the applicant. Blank answers should
   * not count. In general, if a question is not answered, it cannot have errors associated with it.
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
