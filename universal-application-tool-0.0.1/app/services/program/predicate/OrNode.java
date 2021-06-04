package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

/**
 * Represents the boolean operator OR. At least one of the child predicates must evaluate to true
 * for the entire OR tree to be considered true.
 */
@JsonTypeName("or")
@AutoValue
public abstract class OrNode implements ConcretePredicateExpressionNode {

  @JsonIgnore
  public static OrNode create(
      @JsonProperty("children") ImmutableSet<PredicateExpressionNode> children) {
    return new AutoValue_OrNode(children);
  }

  @JsonProperty("children")
  public abstract ImmutableSet<PredicateExpressionNode> children();

  @Override
  @JsonIgnore
  public PredicateExpressionNodeType getType() {
    return PredicateExpressionNodeType.OR;
  }
}
