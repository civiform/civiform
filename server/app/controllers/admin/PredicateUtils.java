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
      ImmutableList<QuestionDefinition> questionDefinitions,
      boolean expandedFormLogicEnabled) {
    String headingSuffix =
        expandedFormLogicEnabled ? "conditions are true:" : "of the following is true:";
    return switch (predicate.predicateFormat()) {
      case SINGLE_CONDITION ->
          ReadablePredicate.create(
              /* heading= */ predicate.toDisplayString(blockName, questionDefinitions),
              /* formattedHtmlHeading= */ predicate.toDisplayFormattedHtml(
                  blockName, questionDefinitions),
              /* conditionList= */ Optional.empty(),
              /* formattedHtmlConditionList= */ Optional.empty());
      case MULTIPLE_CONDITIONS -> {
        String headingPrefix =
            predicate.getPredicateSubject(blockName)
                + " is "
                + predicate.action().toDisplayString();
        UnescapedText formattedHtmlHeadingPrefix =
            join(
                predicate.getPredicateSubject(blockName),
                "is",
                predicate.action().toDisplayFormattedHtml());
        ImmutableList<PredicateExpressionNode> childNodes = predicate.rootNode().getChildren();
        String rootNodeTypeDisplay = predicate.rootNode().getType().toDisplayString();
        String heading = headingPrefix + " " + rootNodeTypeDisplay + " " + headingSuffix;
        UnescapedText formattedHtmlHeading =
            join(formattedHtmlHeadingPrefix, strong(rootNodeTypeDisplay), headingSuffix);
        ImmutableList<String> conditionList =
            childNodes.stream()
                .map(childNode -> childNode.toDisplayString(questionDefinitions))
                .collect(ImmutableList.toImmutableList());
        ImmutableList<UnescapedText> formattedHtmlConditionList =
            childNodes.stream()
                .map(childNode -> childNode.toDisplayFormattedHtml(questionDefinitions))
                .collect(ImmutableList.toImmutableList());
        yield ReadablePredicate.create(
            heading,
            formattedHtmlHeading,
            Optional.of(conditionList),
            Optional.of(formattedHtmlConditionList));
      }
    };
  }

  /** Join HTML components with delimiter inserted between components. */
  public static UnescapedText joinUnescapedText(
      ImmutableList<UnescapedText> components, String delimiter) {
    return components.stream()
        .reduce(
            new UnescapedText(""),
            (first, second) -> {
              // Only insert delimiter when at least 2 elements are present
              return first.toString().isEmpty() ? second : join(first, delimiter, second);
            });
  }
}
