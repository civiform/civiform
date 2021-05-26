package services.applicant.predicate;

import services.applicant.ApplicantData;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.PredicateExpressionNode;

public class PredicateEvaluator {

  private final ApplicantData applicantData;
  private final JsonPathPredicateGenerator predicateGenerator;

  public PredicateEvaluator(
      ApplicantData applicantData, JsonPathPredicateGenerator predicateGenerator) {
    this.applicantData = applicantData;
    this.predicateGenerator = predicateGenerator;
  }

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

  boolean evaluateLeafNode(LeafOperationExpressionNode node) {
    JsonPathPredicate predicate = predicateGenerator.fromLeafNode(node);
    return applicantData.evalPredicate(predicate);
  }
}
