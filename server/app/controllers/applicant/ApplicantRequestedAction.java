package controllers.applicant;

/**
 * An enum representing what page an applicant would like to see after completing their current
 * block.
 */
public enum ApplicantRequestedAction {
  /**
   * The applicant wants to see the next block in the form.
   *
   * <p>Note that "next" can mean either the next *visible* block in the form, or the next
   * *incomplete* block in the form. See {@link
   * ApplicantProgramBlocksController#updateWithApplicantId} for more details.
   */
  NEXT_BLOCK,
  /** The applicant wants to see the review page with all the questions. */
  REVIEW_PAGE;

  /** Returns the action represented by the given string. */
  public static ApplicantRequestedAction getFromString(String action) {
    try {
      return ApplicantRequestedAction.valueOf(action);
    } catch (IllegalArgumentException e) {
      return ApplicantRequestedAction.NEXT_BLOCK;
    }
  }
}
