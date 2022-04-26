package services.applicant.predicate;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.RepeatedEntity;
import services.applicant.exception.InvalidPredicateException;
import services.applicant.question.ApplicantQuestion;
import services.program.predicate.LeafOperationExpressionNode;
import services.question.types.QuestionDefinition;

/** Generates {@link JsonPathPredicate}s based on the current applicant filling out the program. */
public class JsonPathPredicateGenerator {

  private final ImmutableMap<Long, QuestionDefinition> questionsById;
  private final Optional<RepeatedEntity> currentRepeatedContext;

  /**
   * This cannot be built from a set of {@link ApplicantQuestion}s because the question IDs for
   * repeated questions are used multiple times. For example, if there is a repeated name question,
   * that name question is repeated for each entity. Instead, we use {@link QuestionDefinition}s and
   * pass in the current block's repeated entity context, so we can determine which repeated entity
   * we are currently on, as well as find the target question used in the predicate.
   */
  public JsonPathPredicateGenerator(
      ImmutableList<QuestionDefinition> programQuestions,
      Optional<RepeatedEntity> currentRepeatedContext) {
    this.questionsById =
        programQuestions.stream().collect(toImmutableMap(QuestionDefinition::getId, q -> q));
    this.currentRepeatedContext = currentRepeatedContext;
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

    QuestionDefinition targetQuestion = questionsById.get(node.questionId());
    Optional<RepeatedEntity> predicateContext;

    if (targetQuestion.getEnumeratorId().isEmpty()) {
      // This is a top-level question (i.e. is not repeated) - use an empty repeated context.
      predicateContext = Optional.empty();
    } else {
      predicateContext = getTargetContext(targetQuestion);
    }

    Path path =
        targetQuestion.getContextualizedPath(predicateContext, ApplicantData.APPLICANT_PATH);

    if (path.isArrayElement() && targetQuestion.isEnumerator()) {
      // In this case, we don't want the [] at the end of the path.
      path = path.withoutArrayReference();
    }

    return JsonPathPredicate.create(
        String.format(
            "%s[?(@.%s %s %s)]",
            path.predicateFormat(),
            node.scalar().name().toLowerCase(),
            node.operator().toJsonPathOperator(),
            node.comparedValue().value()));
  }

  private Optional<RepeatedEntity> getTargetContext(QuestionDefinition targetQuestion)
      throws InvalidPredicateException {
    // Walk up the RepeatedEntity ancestors to find the right context. We need the context of the
    // question in the predicate definition - that is, the one where the predicate question's
    // enumerator ID matches the context's enumerator ID.
    long enumeratorId = targetQuestion.getEnumeratorId().get();
    Optional<RepeatedEntity> predicateContext = this.currentRepeatedContext;
    while (predicateContext.isPresent()
        && predicateContext.get().enumeratorQuestionDefinition().getId() != enumeratorId) {
      predicateContext = predicateContext.get().parent();
    }

    // If the context is empty here, it means we never found the context of the predicate
    // question. We are trying to depend on an enumerator or repeated question that is not
    // an ancestor of the current block, which is not allowed and should never happen.
    if (predicateContext.isEmpty()) {
      throw new InvalidPredicateException(
          String.format(
              "Enumerator %d is not an ancestor of the current repeated context", enumeratorId));
    }

    return predicateContext;
  }
}
