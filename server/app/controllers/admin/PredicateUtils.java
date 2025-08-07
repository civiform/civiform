package controllers.admin;

import static j2html.TagCreator.join;
import static j2html.TagCreator.strong;

import com.google.common.collect.ImmutableList;
import j2html.tags.UnescapedText;
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
            predicate.toFormattedDisplayString(blockName, questionDefinitions),
            /* conditionList= */ Optional.empty(),
            /* formattedConditionList= */ Optional.empty());
      case OR_OF_SINGLE_LAYER_ANDS:
        String headingPrefix =
            predicate.getPredicateSubject(blockName)
                + " is "
                + predicate.action().toDisplayString();
        UnescapedText formattedHeadingPrefix =
            join(
                predicate.getPredicateSubject(blockName),
                "is",
                predicate.action().toFormattedDisplayString());
        ImmutableList<PredicateExpressionNode> andNodes =
            predicate.rootNode().getOrNode().children();
        if (andNodes.size() == 1) {
          String heading =
              headingPrefix
                  + " "
                  + andNodes.get(0).getAndNode().toDisplayString(questionDefinitions);
          UnescapedText formattedHeading =
              join(
                  formattedHeadingPrefix,
                  andNodes.get(0).getAndNode().toFormattedDisplayString(questionDefinitions));
          return ReadablePredicate.create(
              heading,
              formattedHeading,
              /* conditionList= */ Optional.empty(),
              /* formattedConditionList= */ Optional.empty());
        } else {
          String heading = headingPrefix + " any of the following is true:";
          UnescapedText formattedHeading =
              join(formattedHeadingPrefix, strong("any"), "of the following is true:");
          ImmutableList<String> conditionList =
              andNodes.stream()
                  .map(andNode -> andNode.getAndNode().toDisplayString(questionDefinitions))
                  .collect(ImmutableList.toImmutableList());
          ImmutableList<UnescapedText> formattedConditionList =
              andNodes.stream()
                  .map(
                      andNode -> andNode.getAndNode().toFormattedDisplayString(questionDefinitions))
                  .collect(ImmutableList.toImmutableList());
          return ReadablePredicate.create(
              heading,
              formattedHeading,
              Optional.of(conditionList),
              Optional.of(formattedConditionList));
        }
      default:
        throw new IllegalStateException(
            String.format("Predicate format [%s] not handled", predicate.predicateFormat().name()));
    }
  }

  /** Join formatted components with delimiter inserted between components. */
  public static UnescapedText joinUnescapedText(
      ImmutableList<UnescapedText> components, String delimiter) {
    ImmutableList.Builder<UnescapedText> formattedComponents = ImmutableList.builder();
    components.subList(0, components.size() - 1).stream()
        .forEach(component -> formattedComponents.add(join(component, delimiter)));
    formattedComponents.add(components.get(components.size() - 1));
    return join(formattedComponents.build().toArray());
  }
}
