package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PredicateDefinition {

  @JsonCreator
  public static PredicateDefinition create(
      PredicateExpressionNode rootNode, PredicateAction action) {
    return new AutoValue_PredicateDefinition(rootNode, action);
  }

  @JsonProperty("rootNode")
  public abstract PredicateExpressionNode rootNode();

  @JsonProperty("action")
  public abstract PredicateAction action();
}
