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
    switch (predicate.predicateFormat()) {
      case SINGLE_QUESTION:
        return ReadablePredicate.create(
            /* heading= */ predicate.toDisplayString(blockName, questionDefinitions),
            /* conditionList= */ Optional.empty());
      case OR_OF_SINGLE_LAYER_ANDS:
        String headingPrefix = blockName + " is " + predicate.action().toDisplayString();
        ImmutableList<PredicateExpressionNode> andNodes =
            predicate.rootNode().getOrNode().children();
        if (andNodes.size() == 1) {
          return ReadablePredicate.create(
              /* heading= */ headingPrefix
                  + " "
                  + andNodes.get(0).getAndNode().toDisplayString(questionDefinitions),
              /* conditionList= */ Optional.empty());
        } else {
          String heading = headingPrefix + " any of:";
          ImmutableList<String> conditionList =
              andNodes.stream()
                  .map(andNode -> andNode.getAndNode().toDisplayString(questionDefinitions))
                  .collect(ImmutableList.toImmutableList());

          return ReadablePredicate.create(heading, Optional.of(bullets));
        }
      default:
        throw new IllegalStateException(
            String.format("Predicate format [%s] not handled", predicate.predicateFormat().name()));
    }
  }
}
