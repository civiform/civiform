package views.admin.programs;

import static j2html.TagCreator.div;
import static j2html.TagCreator.li;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;
import static j2html.TagCreator.ul;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.UlTag;
import services.program.ProgramDefinition;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.question.types.QuestionDefinition;
import views.BaseHtmlView;
import views.ViewUtils;
import views.ViewUtils.BadgeStatus;
import views.components.Icons;
import views.style.AdminStyles;

abstract class ProgramBlockView extends BaseHtmlView {

  /** Renders a div with internal/admin program information. */
  protected final DivTag renderProgramInfo(ProgramDefinition programDefinition) {
    DivTag title =
        div(programDefinition.localizedName().getDefault())
            .withId("program-title")
            .withClasses("text-3xl", "pb-3");
    DivTag description =
        div(programDefinition.localizedDescription().getDefault()).withClasses("text-sm");
    DivTag adminNote =
        div()
            .withClasses("text-sm")
            .with(span("Admin note: ").withClasses("font-semibold"))
            .with(span(programDefinition.adminDescription()));

    ButtonTag editDetailsButton =
        ViewUtils.makeSvgTextButton("Edit program details", Icons.EDIT)
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "my-5");
    asRedirectElement(
        editDetailsButton,
        controllers.admin.routes.AdminProgramController.edit(programDefinition.id()).url());

    return div(
            ViewUtils.makeBadge(BadgeStatus.DRAFT),
            title,
            description,
            adminNote,
            editDetailsButton)
        .withClasses("bg-gray-100", "text-gray-800", "shadow-md", "p-8", "pt-4", "-mx-2");
  }

  /** Renders a div presenting the predicate definition for the admin. */
  protected final DivTag renderExistingPredicate(
      String blockName,
      PredicateDefinition predicateDefinition,
      ImmutableList<QuestionDefinition> questionDefinitions) {
    DivTag container = div();

    if (predicateDefinition
        .computePredicateFormat()
        .equals(PredicateDefinition.PredicateFormat.SINGLE_QUESTION)) {
      return container.with(
          text(predicateDefinition.toDisplayString(blockName, questionDefinitions)));
    } else if (!predicateDefinition
        .computePredicateFormat()
        .equals(PredicateDefinition.PredicateFormat.OR_OF_SINGLE_LAYER_ANDS)) {
      throw new IllegalArgumentException(
          String.format(
              "Predicate type %s is unsupported.", predicateDefinition.computePredicateFormat()));
    }

    ImmutableList<PredicateExpressionNode> andNodes =
        predicateDefinition.rootNode().getOrNode().children();

    if (andNodes.size() == 1) {
      return container.with(
          text(
              blockName
                  + " is "
                  + predicateDefinition.action().toDisplayString()
                  + " "
                  + andNodes.get(0).getAndNode().toDisplayString(questionDefinitions)));
    }

    container.with(
        text(blockName + " is " + predicateDefinition.action().toDisplayString() + " any of:"));
    UlTag conditionList = ul().withClasses("list-disc", "m-4");

    andNodes.stream()
        .map(PredicateExpressionNode::getAndNode)
        .forEach(andNode -> conditionList.with(li(andNode.toDisplayString(questionDefinitions))));

    return container.with(conditionList);
  }
}
