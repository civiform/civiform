package services.program.predicate;

/** Common methods that all leaf nodes must implement. */
public interface LeafExpressionNode extends ConcretePredicateExpressionNode {

  /** The question ID referenced by this leaf node. */
  long questionId();
}
