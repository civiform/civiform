package services.applicant.question;

public interface PresentsErrors {
  /**
   * Returns true if values do not meet conditions defined by admins.
   */
  boolean hasQuestionErrors();

  /**
   * Returns true if there is any type specific errors. The validation does not consider
   * admin-defined conditions.
   */
  boolean hasTypeSpecificErrors();
}
