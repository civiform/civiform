package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Arrays;
import javax.inject.Inject;
import play.mvc.Http;
import play.twirl.api.Content;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.ScalarType;
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

  // TODO(#322): Create POST action endpoint for this form.
  private Tag renderPredicateForm(
      String blockName, QuestionDefinition questionDefinition, Tag csrfTag) {

    ImmutableMap<String, String> actionOptions =
        Arrays.stream(PredicateAction.values())
            .collect(toImmutableMap(PredicateAction::toDisplayString, PredicateAction::name));

    ImmutableMap<String, String> operatorOptions =
        Arrays.stream(Operator.values())
            .collect(toImmutableMap(Operator::toDisplayString, Operator::name));

    ImmutableMap<Scalar, ScalarType> scalars;
    try {
      scalars = Scalar.getScalars(questionDefinition.getQuestionType());
    } catch (InvalidQuestionTypeException | UnsupportedQuestionTypeException e) {
      // This should never happen since we filter out Enumerator questions before this point.
      return div()
          .withText("Sorry, you cannot create a show/hide predicate with this question type.");
    }
    ImmutableMap<String, String> scalarOptions =
        scalars.keySet().stream().collect(toImmutableMap(Scalar::toDisplayString, Scalar::name));

    ContainerTag valueField;
    if (questionDefinition.getQuestionType().isMultiOptionType()) {
      // If it's a multi-option question, we need to provide a discrete list of possible values to
      // choose from instead of a freeform text field. Not only is it a better UX, but we store the
      // ID of the options rather than the display strings since the option display strings are
      // localized.
      ImmutableMap<String, String> valueOptions =
          ((MultiOptionQuestionDefinition) questionDefinition)
              .getOptions().stream()
                  .collect(
                      toImmutableMap(
                          option -> option.optionText().getDefault(),
                          option -> String.valueOf(option.id())));

      valueField =
          new SelectWithLabel().setLabelText("Value").setOptions(valueOptions).getContainer();
    } else {
      valueField = FieldWithLabel.input().setLabelText("Value").getContainer();
    }

    return form(csrfTag)
        .withClasses(Styles.FLEX, Styles.FLEX_COL, Styles.GAP_4)
        .with(
            new SelectWithLabel()
                .setLabelText(String.format("%s should be", blockName))
                .setOptions(actionOptions)
                .getContainer())
        .with(renderQuestionDefinitionBox(questionDefinition))
        .with(
            div()
                .withClasses(Styles.FLEX, Styles.FLEX_ROW, Styles.GAP_1)
                .with(
                    new SelectWithLabel()
                        .setId("select-scalar")
                        .setLabelText("Scalar")
                        .setOptions(scalarOptions)
                        .getContainer())
                .with(
                    new SelectWithLabel()
                        .setLabelText("Operator")
                        // TODO(#322): Display the right operators for the given scalar type
                        //  (requires javascript).
                        .setOptions(operatorOptions)
                        .getContainer())
                .with(valueField));
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
