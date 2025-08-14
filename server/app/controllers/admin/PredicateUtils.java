package controllers.admin;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.question.types.QuestionDefinition;

/** Utility methods for working with predicates on the admin side. */
public final class PredicateUtils {
  /** Returns the predicate in a human-readable form for admins. */
  public static ReadablePredicate getReadablePredicateDescription(
      String blockName,
      PredicateDefinition predicate,
      ImmutableList<QuestionDefinition> questionDefinitions) {
    return switch (predicate.predicateFormat()) {
      case SINGLE_QUESTION -> {
        yield ReadablePredicate.create(
            /* heading= */ predicate.toDisplayString(blockName, questionDefinitions),
            /* conditionList= */ Optional.empty());
      }
      case OR_OF_SINGLE_LAYER_ANDS -> {
        String headingPrefix = blockName + " is " + predicate.action().toDisplayString();
        ImmutableList<PredicateExpressionNode> andNodes =
            predicate.rootNode().getOrNode().children();
        if (andNodes.size() == 1) {
          yield ReadablePredicate.create(
              /* heading= */ "%s %s"
                  .formatted(
                      headingPrefix,
                      andNodes.get(0).getAndNode().toDisplayString(questionDefinitions)),
              /* conditionList= */ Optional.empty());
        }
        String heading = headingPrefix + " any of:";
        ImmutableList<String> conditionList =
            andNodes.stream()
                .map(andNode -> andNode.getAndNode().toDisplayString(questionDefinitions))
                .collect(ImmutableList.toImmutableList());
        yield ReadablePredicate.create(heading, Optional.of(conditionList));
      }
    };
  }
}
