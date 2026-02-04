package services.program.predicate;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.join;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import j2html.tags.UnescapedText;
import services.question.types.QuestionDefinition;

/**
 * Conditional logic associated with a single program screen (block). This may be used to (for
 * example) show or hide a block based on the answers an applicant provides.
 *
 * <p>The class is annotated to ignore "predicateFormat" as a JSON property due to a previous
 * version of the code that stored the predicate format in the database rather than computing it
 * from the expression tree.
 */
@AutoValue
@JsonIgnoreProperties(value = {"predicateFormat"})
public abstract class PredicateDefinition {

  /** Indicates the shape of the predicate's AST so view code can render the appropriate UI. */
  public enum PredicateFormat {
    // A root AND/OR node with only one child node. This child node may itself have children but the
    // root node has only one child and can be rendered in one line as a single statement.
    SINGLE_CONDITION,
    // A root AND/OR node with multiple child nodes. This may be rendered as a list with an item for
    // each child condition.
    MULTIPLE_CONDITIONS;
  }

  @JsonCreator
  public static PredicateDefinition create(
      @JsonProperty("rootNode") PredicateExpressionNode rootNode,
      @JsonProperty("action") PredicateAction action) {
    return new AutoValue_PredicateDefinition(rootNode, action);
  }

  /** Determines what {@link PredicateFormat} a given predicate expression tree is. */
  public static PredicateFormat detectPredicateFormat(PredicateExpressionNode rootNode) {
    return rootNode.getChildren().size() > 1
        ? PredicateFormat.MULTIPLE_CONDITIONS
        : PredicateFormat.SINGLE_CONDITION;
  }

  @JsonProperty("rootNode")
  public abstract PredicateExpressionNode rootNode();

  @JsonProperty("action")
  public abstract PredicateAction action();

  /**
   * Returns the question IDs referenced by this predicate, deduplicated but presented in a list to
   * preserve ordering.
   */
  @JsonIgnore
  @Memoized
  public ImmutableList<Long> getQuestions() {
    return rootNode().getQuestions().stream().distinct().collect(toImmutableList());
  }

  /** Indicates the shape of the predicate's AST so view code can render the appropriate UI. */
  public PredicateFormat predicateFormat() {
    return detectPredicateFormat(rootNode());
  }

  /**
   * Formats this predicate definition as a human-readable sentence, in the format "[applicant or
   * block name] is [eligible or hidden or shown if] [predicate expression]" - ex: "My Block is
   * hidden if applicant address's city is equal to 'Seattle'".
   *
   * <p>This should only be used when a plain string representation is necessary, such as for PDF
   * Export. Most user-facing application use cases should use {@link
   * #toDisplayFormattedHtml(String, ImmutableList)} instead.
   */
  public String toDisplayString(String blockName, ImmutableList<QuestionDefinition> questions) {
    return Joiner.on(' ')
        .join(
            getPredicateSubject(blockName),
            "is",
            action().toDisplayString(),
            rootNode().toDisplayString(questions));
  }

  /**
   * Formats this predicate definition as a human-readable sentence in HTML, in the format
   * "[applicant or block name] is <strong>[eligible or hidden or shown if]</strong> [predicate
   * expression]" - ex: "My Block is <strong>hidden</strong> if applicant address's city is equal to
   * <strong>'Seattle'</strong>".
   */
  public UnescapedText toDisplayFormattedHtml(
      String blockName, ImmutableList<QuestionDefinition> questions) {
    return join(
        getPredicateSubject(blockName),
        "is",
        action().toDisplayFormattedHtml(),
        rootNode().toDisplayFormattedHtml(questions));
  }

  /**
   * Calculates the subject of the predicate. For eligible blocks, the subject is the applicant. For
   * all other predicates, the subject is the block name.
   */
  public String getPredicateSubject(String blockName) {
    return action().equals(PredicateAction.ELIGIBLE_BLOCK) ? "Applicant" : blockName;
  }
}
