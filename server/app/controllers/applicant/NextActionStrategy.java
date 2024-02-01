package controllers.applicant;

public enum NextActionStrategy {
    NEXT_VISIBLE,
    NEXT_INCOMPLETE;

    public static NextActionStrategy getFromString(String nextAction) {
        try {
            return NextActionStrategy.valueOf(nextAction);
        } catch (IllegalArgumentException e) {
            // If the string is malformatted, default to the next visible.
            // TODO: Log
            return NextActionStrategy.NEXT_VISIBLE;
        }
    }
}
