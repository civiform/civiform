package services.applicant.question;

public interface PresentsErrors {
  /** Returns true if values do not meet conditions defined by admins. */
  boolean hasQuestionErrors();

  /**
   * Returns true if there is any type specific errors. The validation does not consider
   * admin-defined conditions.
   */
  boolean hasTypeSpecificErrors();

  /**
   * Returns true if the question has been answered by the applicant, even if that answer was blank.
   */
  boolean isAnswered();
}
