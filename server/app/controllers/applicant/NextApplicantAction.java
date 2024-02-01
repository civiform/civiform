package controllers.applicant;

/**
 * An enum describing what action an applicant wants to take after finishing a specific screen in an
 * application.
 */
public enum NextApplicantAction {
  /**
   * The applicant wants to see the next incomplete block. This should be used if the user is
   * reviewing their application and just needs to fill in the last missing answers.
   */
  NEXT_INCOMPLETE,
  /**
   * The applicant wants to see the next visible block. This should be used if the user is
   * starting/continuing an application.
   */
  NEXT_VISIBLE,
  /**
   * The applicant wants to see the previous block. If there is no previous block (e.g. the applicant is currently on the first block),
   * they'll be taken to the review page. */
  PREVIOUS,
  /** The applicant wants to see the review page with all questions listed. */
  REVIEW_PAGE;

  public static NextApplicantAction getActionFromString(String nextAction) {
    try {
      return NextApplicantAction.valueOf(nextAction);
    } catch (IllegalArgumentException e) {
      // If the string is malformatted, default to the next visible.
      // TODO: Log
      return NextApplicantAction.NEXT_VISIBLE;
    }
  }
}
