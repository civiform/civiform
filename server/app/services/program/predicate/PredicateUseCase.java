package services.program.predicate;

/** The supported use cases for predicate expressions. */
public enum PredicateUseCase {
  ELIGIBILITY("eligibility"),
  VISIBILITY("visibility");

  private final String displayString;

  PredicateUseCase(String displayString) {
    this.displayString = displayString;
  }

  public String toDisplayString() {
    return displayString;
  }
}
