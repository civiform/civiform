package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableList;
import j2html.tags.UnescapedText;
import services.question.types.QuestionDefinition;

/** Common methods that all predicate node types need to implement. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AndNode.class, name = "and"),
  @JsonSubTypes.Type(value = LeafOperationExpressionNode.class, name = "leaf"),
  @JsonSubTypes.Type(
      value = LeafAddressServiceAreaExpressionNode.class,
      name = "leafAddressServiceArea"),
  @JsonSubTypes.Type(value = OrNode.class, name = "or")
})
public interface ConcretePredicateExpressionNode {

  /** Returns the type of this node, as a {@link PredicateExpressionNodeType}. */
  PredicateExpressionNodeType getType();

  /**
   * Returns a human-readable representation of this node. This should only be used when a plain
   * string representation is necessary, such as for PDF Export. Most user-facing application use
   * cases should use {@link #toDisplayFormattedHtml(ImmutableList)} instead.
   *
   * <p>The list of questions is used to determine the context for particular expressions - for
   * example, translating option IDs to human-readable text for multi-option questions.
   * Additionally, question names are used in the expression phrase. If the predicate question is
   * not found, the name is omitted.
   *
   * <p>We pass in a list rather than a single question, since some nodes (such as AND/OR) may use
   * several different questions.
   *
   * <p>If the list of questions does not contain the predicate questions, then we cannot display
   * the human-readable text for multi-option answers. Instead, the ID for the option is shown.
   *
   * @param questions the list of questions this predicate may use
   * @return a human-readable representation of this node
   */
  String toDisplayString(ImmutableList<QuestionDefinition> questions);

  /**
   * Returns a formatted, human-readable representation of this node in HTML format. Identical to
   * {@link #toDisplayString(ImmutableList)} with the exception that the display string may have
   * additional HTML formatting such as <strong> tags to emphasize certain parts of the predicate.
   *
   * @param questions the list of questions this predicate may use
   * @return a formatted, human-readable representation of this node
   */
  UnescapedText toDisplayFormattedHtml(ImmutableList<QuestionDefinition> questions);

  void accept(PredicateExpressionNodeVisitor v);
}
