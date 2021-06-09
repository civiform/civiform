package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.AbstractMap.SimpleEntry;
import javax.inject.Inject;
import play.mvc.Http;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.Modal;
import views.components.SelectWithLabel;
import views.style.AdminStyles;
import views.style.Styles;

public class ProgramBlockPredicatesEditView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramBlockPredicatesEditView(AdminLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(
      Http.Request request,
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      ImmutableList<QuestionDefinition> potentialPredicateQuestions) {
    String title = String.format("Add a condition to show or hide %s", blockDefinition.name());

    Tag csrfTag = makeCsrfTokenInputTag(request);
    ImmutableList<Modal> modals =
        predicateFormModals(blockDefinition.name(), potentialPredicateQuestions, csrfTag);

    ContainerTag content =
        div()
            .withClasses(Styles.MX_6, Styles.MY_10)
            .with(h1().withText(title).withClasses(Styles.MY_4, Styles.FONT_BOLD, Styles.TEXT_XL))
            .with(renderPredicateModalTriggerButtons(modals));

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(
                form().with(csrfTag), layout.renderProgramInfo(programDefinition), content);

    for (Modal modal : modals) {
      htmlBundle = htmlBundle.addModals(modal);
    }

    return layout.renderCentered(htmlBundle);
  }

  private ImmutableList<Modal> predicateFormModals(
      String blockName, ImmutableList<QuestionDefinition> questionDefinitions, Tag csrfTag) {
    ImmutableList.Builder<Modal> builder = ImmutableList.builder();
    for (QuestionDefinition qd : questionDefinitions) {
      builder.add(predicateFormModal(blockName, qd, csrfTag));
    }
    return builder.build();
  }

  private Modal predicateFormModal(
      String blockName, QuestionDefinition questionDefinition, Tag csrfTag) {
    Tag triggerButtonContent =
        div()
            .withClasses(Styles.FLEX, Styles.FLEX_ROW, Styles.GAP_4)
            .with(
                Icons.questionTypeSvg(questionDefinition.getQuestionType(), 24)
                    .withClasses(Styles.FLEX_SHRINK_0, Styles.H_12, Styles.W_6))
            .with(
                div()
                    .withClasses()
                    .with(
                        div(questionDefinition.getName()),
                        div(questionDefinition.getDescription())
                            .withClasses(Styles.MT_1, Styles.TEXT_SM)));

    ContainerTag modalContent =
        div()
            .withClasses(Styles.M_4)
            .with(renderPredicateForm(blockName, questionDefinition, csrfTag));

    return Modal.builder(
            String.format("predicate-modal-%s", questionDefinition.getId()), modalContent)
        .setModalTitle(String.format("Add a condition to show or hide %s", blockName))
        .setTriggerButtonContent(triggerButtonContent)
        .setTriggerButtonStyles(AdminStyles.BUTTON_QUESTION_PREDICATE)
        .build();
  }

  private Tag renderPredicateForm(
      String blockName, QuestionDefinition questionDefinition, Tag csrfTag) {
    return form(csrfTag)
        .with(
            new SelectWithLabel()
                .setLabelText(String.format("%s should be", blockName))
                .setOptions(
                    ImmutableList.of(
                        new SimpleEntry<>("hidden if", "hide"),
                        new SimpleEntry<>("shown if", "show")))
                .getContainer())
        .with(renderQuestionDefinitionBox(questionDefinition))
        .with(
            div()
                .withClasses(Styles.FLEX, Styles.FLEX_ROW, Styles.GAP_1)
                .with(
                    new SelectWithLabel()
                        .setLabelText("Scalar")
                        .setOptions(
                            ImmutableList.of(
                                new SimpleEntry<>("street", "street"),
                                new SimpleEntry<>("city", "city")))
                        .getContainer())
                .with(
                    new SelectWithLabel()
                        .setLabelText("Operator")
                        .setOptions(
                            ImmutableList.of(
                                new SimpleEntry<>("==", "is equal to"),
                                new SimpleEntry<>(">", "is greater than")))
                        .getContainer())
                .with(FieldWithLabel.input().setLabelText("Value").getContainer()));
  }

  private ContainerTag renderPredicateModalTriggerButtons(ImmutableList<Modal> modals) {
    return div()
        .withClasses(Styles.FLEX, Styles.FLEX_COL, Styles.GAP_2)
        .with(each(modals, modal -> modal.getButton()));
  }

  private ContainerTag renderQuestionDefinitionBox(QuestionDefinition questionDefinition) {
    return div()
        .withClasses(
            Styles.FLEX,
            Styles.FLEX_ROW,
            Styles.GAP_4,
            Styles.PX_4,
            Styles.PY_2,
            Styles.BORDER,
            Styles.BORDER_GRAY_200)
        .with(
            Icons.questionTypeSvg(questionDefinition.getQuestionType(), 24)
                .withClasses(Styles.FLEX_SHRINK_0, Styles.H_12, Styles.W_6))
        .with(
            div()
                .withClasses()
                .with(
                    div(questionDefinition.getName()),
                    div(questionDefinition.getDescription())
                        .withClasses(Styles.MT_1, Styles.TEXT_SM)));
  }
}
