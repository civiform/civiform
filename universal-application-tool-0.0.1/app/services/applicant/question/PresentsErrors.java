package services.applicant.question;

import com.google.common.collect.ImmutableSet;
import play.i18n.Messages;

public interface PresentsErrors {
  /** Returns true if values do not meet conditions defined by admins. */
  boolean hasQuestionErrors(Messages messages);

  ImmutableSet<String> getQuestionErrors(Messages messages);

  /**
   * Returns true if there is any type specific errors. The validation does not consider
   * admin-defined conditions.
   */
  boolean hasTypeSpecificErrors(Messages messages);

  ImmutableSet<String> getAllTypeSpecificErrors(Messages messages);

  /**
   * Returns true if the question has been answered by the applicant, even if that answer was blank.
   * In general, if a question is not answered, it cannot have errors associated with it.
   */
  boolean isAnswered();
}
