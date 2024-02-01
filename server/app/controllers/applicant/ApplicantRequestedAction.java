package controllers.applicant;

/**
 * An enum describing what action an applicant wants to take after finishing a specific screen in an
 * application.
 */
public enum ApplicantRequestedAction {
    /**
     * TODO
     */
    NEXT,
    PREVIOUS,
    /** The applicant wants to see the review page with all questions listed. */
    REVIEW_PAGE;


    public static ApplicantRequestedAction getFromString(String requestedAction) {
        try {
            return ApplicantRequestedAction.valueOf(requestedAction);
        } catch (IllegalArgumentException e) {
            // If the string is malformatted, default to the next visible.
            // TODO: Log
            return ApplicantRequestedAction.NEXT;
        }
    }
}
