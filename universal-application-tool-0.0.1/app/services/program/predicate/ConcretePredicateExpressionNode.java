package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableList;
import services.question.types.QuestionDefinition;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = LeafOperationExpressionNode.class, name = "leaf")})
public interface ConcretePredicateExpressionNode {

  /** Returns the type of this node, as a {@link PredicateExpressionNodeType}. */
  PredicateExpressionNodeType getType();

  /** Returns a human-readable interpretation of this node. */
  String toDisplayString(ImmutableList<QuestionDefinition> questions);
}
