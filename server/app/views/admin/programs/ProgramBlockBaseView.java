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
import views.ViewUtils.ProgramDisplayType;
import views.components.Icons;
import views.style.AdminStyles;

abstract class ProgramBlockBaseView extends BaseHtmlView {

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
    return div(ViewUtils.makeBadge(getProgramDisplayStatus()), title, description, adminNote)
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
    UlTag conditionList = ul().withClasses("list-disc", "ml-4", "mb-4");

    andNodes.stream()
        .map(PredicateExpressionNode::getAndNode)
        .forEach(andNode -> conditionList.with(li(andNode.toDisplayString(questionDefinitions))));

    return container.with(conditionList);
  }

  /**
   * Returns a standardized Edit Button that can be added to the program info. A typical use case
   * would be for a subclass to:
   *
   * <ul>
   *   <li>create a button with this method
   *   <li>add subclass specific navigation behaviour to the button
   *   <li>add the button to the program info after creating it with renderProductInfo()
   * </ul>
   */
  protected ButtonTag getStandardizedEditButton(String buttonText) {
    return ViewUtils.makeSvgTextButton(buttonText, Icons.EDIT)
        .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "my-5")
        .withId("header_edit_button");
  }

  /**
   * Returns the Program display type which represents the status of the program. It will be shown
   * at the top of the page.
   */
  protected abstract ProgramDisplayType getProgramDisplayStatus();
}
