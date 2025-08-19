package services.program.predicate;

import static j2html.TagCreator.join;
import static j2html.TagCreator.strong;

import j2html.tags.UnescapedText;

/** Action that is taken when associating predicate is evaluated to be true. */
public enum PredicateAction {
  /** If the predicate evaluates to true, the program is eligible as of the current block. */
  ELIGIBLE_BLOCK("eligible"),
  /** If the predicate evaluates to true, hide the current block. */
  HIDE_BLOCK("hidden"),
  /** If the predicate evaluates to true, show the current block. */
  SHOW_BLOCK("shown");

  private final String actionString;

  PredicateAction(String actionString) {
    this.actionString = actionString;
  }

  public String toDisplayString() {
    return this.actionString + " if";
  }

  public UnescapedText toDisplayFormattedHtml() {
    return join(strong(this.actionString), "if");
  }
}
