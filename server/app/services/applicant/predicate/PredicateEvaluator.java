package services.applicant.predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.applicant.ApplicantData;
import services.applicant.exception.InvalidPredicateException;
import services.program.predicate.AndNode;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.OrNode;
import services.program.predicate.PredicateExpressionNode;

/** Evaluates complex predicates based on the given {@link ApplicantData}. */
public final class PredicateEvaluator {

  private static final Logger LOGGER = LoggerFactory.getLogger(PredicateEvaluator.class);

  private final ApplicantData applicantData;
  private final JsonPathPredicateGenerator predicateGenerator;

  public PredicateEvaluator(
      ApplicantData applicantData, JsonPathPredicateGenerator predicateGenerator) {
    this.applicantData = applicantData;
    this.predicateGenerator = predicateGenerator;
  }

  /**
   * Evaluate an expression tree rooted at the given {@link PredicateExpressionNode}. Will return
   * true if and only if the entire tree evaluates to true based on the {@link ApplicantData} used
   * to create this evaluator.
   */
  public boolean evaluate(PredicateExpressionNode node) {
    switch (node.getType()) {
      case LEAF_OPERATION:
        return evaluateLeafNode(node.getLeafOperationNode());
      case LEAF_ADDRESS_SERVICE_AREA:
        return evaluateLeafAddressServiceAreaNode(node.getLeafAddressNode());
      case AND:
        return evaluateAndNode(node.getAndNode());
      case OR:
        return evaluateOrNode(node.getOrNode());
      default:
        return false;
    }
  }

  /**
   * Returns true if and only if there exists a value in {@link ApplicantData} that satisfies the
   * given leaf node operation. Returns false if the predicate is invalid or the predicate evaluates
   * to false.
   */
  private boolean evaluateLeafNode(LeafOperationExpressionNode node) {
    try {
      JsonPathPredicate predicate = predicateGenerator.fromLeafNode(node);
      return applicantData.evalPredicate(predicate);
    } catch (InvalidPredicateException e) {
      LOGGER.error(
          "InvalidPredicateException when evaluating LeafOperationExpressionNode {}: {}",
          node,
          e.getMessage());
      return false;
    }
  }

  /**
   * Returns true if and only if the answer for the address question referenced by the {@link
   * LeafAddressServiceAreaExpressionNode} has an in-area or failed service area in {@link
   * ApplicantData}.
   */
  private boolean evaluateLeafAddressServiceAreaNode(LeafAddressServiceAreaExpressionNode node) {
    try {
      JsonPathPredicate predicate = predicateGenerator.fromLeafAddressServiceAreaNode(node);
      return applicantData.evalPredicate(predicate);
    } catch (InvalidPredicateException e) {
      LOGGER.error(
          "InvalidPredicateException when evaluating LeafAddressServiceAreaExpressionNode {}: {}",
          node,
          e.getMessage());
      return false;
    }
  }

  /** Returns true if and only if each of the node's children evaluates to true. */
  private boolean evaluateAndNode(AndNode node) {
    return node.children().stream().allMatch(this::evaluate);
  }

  /** Returns true if and only if one or more of the node's children evaluates to true. */
  private boolean evaluateOrNode(OrNode node) {
    return node.children().stream().anyMatch(this::evaluate);
  }
}
