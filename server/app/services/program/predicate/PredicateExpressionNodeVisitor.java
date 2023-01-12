package services.program.predicate;

/**
 * Visitor pattern (https://en.wikipedia.org/wiki/Visitor_pattern) for traversing predicate ASTs.
 */
public abstract class PredicateExpressionNodeVisitor {

  public void visit(AndNode andNode) {}

  public void visit(OrNode orNode) {}

  public void visit(LeafOperationExpressionNode leafOperationExpressionNode) {}

  public void visit(LeafAddressServiceAreaExpressionNode leafAddressServiceAreaExpressionNode) {}
}
