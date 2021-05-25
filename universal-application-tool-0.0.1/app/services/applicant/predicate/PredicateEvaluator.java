package services.applicant.predicate;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.applicant.question.ApplicantQuestion;

public class PredicateEvaluator {

  private final ImmutableList<ApplicantQuestion> applicantQuestions;

  public PredicateEvaluator(ImmutableList<ApplicantQuestion> applicantQuestions) {
    this.applicantQuestions = applicantQuestions;
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
    Optional<ApplicantQuestion> question =
        applicantQuestions.stream()
            .filter(q -> q.getQuestionDefinition().getId() == node.questionId())
            .findFirst();
    if (question.isEmpty()) {
      return false;
    }
    JsonPathPredicate predicate = node.toJsonPathPredicate(question.get().getContextualizedPath());
    return question.get().getApplicantData().evalPredicate(predicate);
  }
}
