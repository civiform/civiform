package services.applicant.predicate;

import com.google.common.collect.ImmutableList;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.PredicateExpressionNode;

public class PredicateEvaluator {

  private final ApplicantData applicantData;
  private final ImmutableList<ApplicantQuestion> applicantQuestions;
  private final JsonPathPredicateGenerator predicateGenerator;

  public PredicateEvaluator(
      ApplicantData applicantData, ImmutableList<ApplicantQuestion> applicantQuestions) {
    this.applicantData = applicantData;
    this.applicantQuestions = applicantQuestions;
    this.predicateGenerator = new JsonPathPredicateGenerator(applicantQuestions);
  }

  public boolean evaluate(PredicateExpressionNode node) {
    switch (node.getType()) {
      case LEAF_OPERATION:
        return evaluateLeafNode(node.getLeafNode());
      case AND: // fallthrough intended
      case OR:
      default:
        return false;
    }
  }

  boolean evaluateLeafNode(LeafOperationExpressionNode node) {
    JsonPathPredicate predicate = predicateGenerator.fromLeafNode(node);
    return applicantData.evalPredicate(predicate);
  }
}
