package services.applicant.predicate;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.applicant.question.ApplicantQuestion;
import services.program.predicate.LeafOperationExpressionNode;

public class JsonPathPredicateGenerator {

  private final ImmutableList<ApplicantQuestion> programQuestions;

  public JsonPathPredicateGenerator(ImmutableList<ApplicantQuestion> programQuestions) {
    this.programQuestions = programQuestions;
  }

  /**
   * Formats a {@link LeafOperationExpressionNode} in JsonPath format: {@code path[?(expression)]}
   *
   * <p>Example: \$.applicant.address[?(@.zip in ["12345", "56789"])]
   */
  public JsonPathPredicate fromLeafNode(LeafOperationExpressionNode node) {
    Optional<ApplicantQuestion> question =
        programQuestions.stream()
            .filter(q -> q.getQuestionDefinition().getId() == node.questionId())
            .findFirst();
    if (question.isEmpty()) {
      throw new RuntimeException(
          String.format(
              "Tried to apply a predicated based on question %d, which is not found in this"
                  + " program.",
              node.questionId()));
    }

    return JsonPathPredicate.create(
        String.format(
            "%s[?(@.%s %s %s)]",
            question.get().getContextualizedPath().predicateFormat(),
            node.scalar().name().toLowerCase(),
            node.operator().toJsonPathOperator(),
            node.comparedValue().value()));
  }
}
