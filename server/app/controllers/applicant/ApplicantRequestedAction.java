package controllers.applicant;

public enum ApplicantRequestedAction {
  NEXT,
  REVIEW_PAGE;

  public static ApplicantRequestedAction getFromString(String action) {
    try {
      return ApplicantRequestedAction.valueOf(action);
    } catch (IllegalArgumentException e) {
      return ApplicantRequestedAction.NEXT;
    }
  }
}
