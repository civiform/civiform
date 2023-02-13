package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableList;
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
   * Returns a human-readable representation of this node. The list of questions is used to
   * determine the context for particular expressions - for example, translating option IDs to
   * human-readable text for multi-option questions. Additionally, question names are used in the
   * expression phrase. If the predicate question is not found, the name is omitted.
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

  void accept(PredicateExpressionNodeVisitor v);
}
