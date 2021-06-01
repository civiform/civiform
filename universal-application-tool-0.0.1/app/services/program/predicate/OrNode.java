package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

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
