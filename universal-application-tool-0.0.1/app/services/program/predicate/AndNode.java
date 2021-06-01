package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

@JsonTypeName("and")
@AutoValue
public abstract class AndNode implements ConcretePredicateExpressionNode {

  @JsonCreator
  public static AndNode create(
      @JsonProperty("children") ImmutableSet<PredicateExpressionNode> children) {
    return new AutoValue_AndNode(children);
  }

  @JsonProperty("children")
  public abstract ImmutableSet<PredicateExpressionNode> children();

  @Override
  @JsonIgnore
  public PredicateExpressionNodeType getType() {
    return PredicateExpressionNodeType.AND;
  }
}
