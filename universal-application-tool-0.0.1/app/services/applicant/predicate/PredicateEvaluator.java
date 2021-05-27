package services.applicant.predicate;

import services.applicant.ApplicantData;
import services.applicant.exception.InvalidPredicateException;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.PredicateExpressionNode;

/** Evaluates complex predicates based on the given {@link ApplicantData}. */
public class PredicateEvaluator {

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
   * to create this generator.
   */
  public boolean evaluate(PredicateExpressionNode node) {
    switch (node.getType()) {
      case LEAF_OPERATION:
        return evaluateLeafNode(node.getLeafNode());
      case AND: // fallthrough intended
      case OR: // fallthrough intended
      default:
        return false;
    }
  }

  /**
   * Returns true if and only if there exists a value in {@link ApplicantData} that satisfies the
   * given leaf node operation. If the path does not exist or the value at the path does not satisfy
   * the expression, this method returns false.
   */
  private boolean evaluateLeafNode(LeafOperationExpressionNode node) {
    try {
      JsonPathPredicate predicate = predicateGenerator.fromLeafNode(node);
      return applicantData.evalPredicate(predicate);
    } catch (InvalidPredicateException e) {
      return false;
    }
  }
}
