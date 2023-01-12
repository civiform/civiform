package services.program.predicate;

/**
 * Visitor pattern (https://en.wikipedia.org/wiki/Visitor_pattern) for traversing predicate ASTs.
 */
public interface PredicateExpressionNodeVisitor {

  void visit(AndNode andNode);

  void visit(OrNode orNode);

  void visit(LeafOperationExpressionNode leafOperationExpressionNode);

  void visit(LeafAddressServiceAreaExpressionNode leafAddressServiceAreaExpressionNode);
}
