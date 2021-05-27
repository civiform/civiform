package services.applicant.predicate;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import services.applicant.exception.InvalidPredicateException;
import services.applicant.question.ApplicantQuestion;
import services.program.predicate.LeafOperationExpressionNode;

/** Generates {@link JsonPathPredicate}s based on the current applicant filling out the program. */
public class JsonPathPredicateGenerator {

  private final ImmutableMap<Long, ApplicantQuestion> questionsById;

  public JsonPathPredicateGenerator(ImmutableList<ApplicantQuestion> programQuestions) {
    this.questionsById =
        programQuestions.stream()
            .collect(toImmutableMap(q -> q.getQuestionDefinition().getId(), q -> q));
  }

  /**
   * Formats a {@link LeafOperationExpressionNode} in JsonPath format: {@code path[?(expression)]}
   *
   * <p>Example: \$.applicant.address[?(@.zip in ["12345", "56789"])]
   */
  public JsonPathPredicate fromLeafNode(LeafOperationExpressionNode node)
      throws InvalidPredicateException {
    if (!questionsById.containsKey(node.questionId())) {
      // This means a predicate was incorrectly configured - we are depending upon a question that
      // does not appear anywhere in this program.
      throw new InvalidPredicateException(
          String.format(
              "Tried to apply a predicate based on question %d, which is not found in this"
                  + " program.",
              node.questionId()));
    }

    return JsonPathPredicate.create(
        String.format(
            "%s[?(@.%s %s %s)]",
            questionsById.get(node.questionId()).getContextualizedPath().predicateFormat(),
            node.scalar().name().toLowerCase(),
            node.operator().toJsonPathOperator(),
            node.comparedValue().value()));
  }
}
