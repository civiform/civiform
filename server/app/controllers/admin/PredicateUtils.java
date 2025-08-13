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
            /* formattedHtmlHeading= */ predicate.toDisplayFormattedHtml(
                blockName, questionDefinitions),
            /* conditionList= */ Optional.empty(),
            /* formattedHtmlConditionList= */ Optional.empty());
      case OR_OF_SINGLE_LAYER_ANDS:
        String headingPrefix =
            predicate.getPredicateSubject(blockName)
                + " is "
                + predicate.action().toDisplayString();
        UnescapedText formattedHtmlHeadingPrefix =
            join(
                predicate.getPredicateSubject(blockName),
                "is",
                predicate.action().toDisplayFormattedHtml());
        ImmutableList<PredicateExpressionNode> andNodes =
            predicate.rootNode().getOrNode().children();
        if (andNodes.size() == 1) {
          String heading =
              headingPrefix
                  + " "
                  + andNodes.get(0).getAndNode().toDisplayString(questionDefinitions);
          UnescapedText formattedHeading =
              join(
                  formattedHtmlHeadingPrefix,
                  andNodes.get(0).getAndNode().toDisplayFormattedHtml(questionDefinitions));
          return ReadablePredicate.create(
              heading,
              formattedHeading,
              /* conditionList= */ Optional.empty(),
              /* formattedHtmlConditionList= */ Optional.empty());
        } else {
          String heading = headingPrefix + " any of the following is true:";
          UnescapedText formattedHtmlHeading =
              join(formattedHtmlHeadingPrefix, strong("any"), "of the following is true:");
          ImmutableList<String> conditionList =
              andNodes.stream()
                  .map(andNode -> andNode.getAndNode().toDisplayString(questionDefinitions))
                  .collect(ImmutableList.toImmutableList());
          ImmutableList<UnescapedText> formattedHtmlConditionList =
              andNodes.stream()
                  .map(andNode -> andNode.getAndNode().toDisplayFormattedHtml(questionDefinitions))
                  .collect(ImmutableList.toImmutableList());
          return ReadablePredicate.create(
              heading,
              formattedHtmlHeading,
              Optional.of(conditionList),
              Optional.of(formattedHtmlConditionList));
        }
      default:
        throw new IllegalStateException(
            String.format("Predicate format [%s] not handled", predicate.predicateFormat().name()));
    }
  }

  /** Join formatted HTML components with delimiter inserted between components. */
  public static UnescapedText joinUnescapedText(
      ImmutableList<UnescapedText> components, String delimiter) {
    if (components.size() == 1) {
      return components.get(0);
    }

    return components.subList(1, components.size()).stream()
        .reduce(join(components.get(0)), (first, second) -> join(first, delimiter, second));
  }
}
