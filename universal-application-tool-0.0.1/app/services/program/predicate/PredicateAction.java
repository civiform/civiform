package services.program.predicate;

/** Action that is taken when associating predicate is evaluated to be true. */
public enum PredicateAction {
  /** If the predicate evaluates to true, hide the current block. */
  HIDE_BLOCK("hidden if"),
  /** If the predicate evaluates to true, show the current block. */
  SHOW_BLOCK("shown if");

  private final String displayString;

  PredicateAction(String displayString) {
    this.displayString = displayString;
  }

  public String toDisplayString() {
    return this.displayString;
  }
}
