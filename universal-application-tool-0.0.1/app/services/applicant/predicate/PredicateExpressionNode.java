package services.program.predicate;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PredicateExpressionNode {

  public static PredicateExpressionNode create() {
    return new AutoValue_PredicateExpressionNode();
  }

  public abstract PredicateExpressionNodeType getType();
}
