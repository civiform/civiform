package services.program.predicate;

import com.google.common.collect.ImmutableList;

/** Extracts {@link LeafAddressServiceAreaExpressionNode}s from a {@link PredicateDefinition}. */
public final class PredicateAddressServiceAreaNodeExtractor
    implements PredicateExpressionNodeVisitor {

  private ImmutableList.Builder<LeafAddressServiceAreaExpressionNode> nodes =
      ImmutableList.builder();

  /** Extracts {@link LeafAddressServiceAreaExpressionNode}s from a {@link PredicateDefinition}. */
  public static ImmutableList<LeafAddressServiceAreaExpressionNode> extract(
      PredicateDefinition predicateDefinition) {
    return new PredicateAddressServiceAreaNodeExtractor().extractInternal(predicateDefinition);
  }

  private ImmutableList<LeafAddressServiceAreaExpressionNode> extractInternal(
      PredicateDefinition predicateDefinition) {
    predicateDefinition.rootNode().accept(this);
    return nodes.build();
  }

  @Override
  public void visit(LeafAddressServiceAreaExpressionNode leafAddressServiceAreaExpressionNode) {
    this.nodes.add(leafAddressServiceAreaExpressionNode);
  }

  @Override
  public void visit(AndNode andNode) {}

  @Override
  public void visit(OrNode orNode) {}

  @Override
  public void visit(LeafOperationExpressionNode leafOperationExpressionNode) {}
}
