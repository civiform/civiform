package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.input;
import static j2html.TagCreator.option;
import static j2html.TagCreator.text;
import static play.mvc.Http.HttpVerbs.POST;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import controllers.admin.routes;
import j2html.attributes.Attr;
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
import services.question.QuestionOption;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.SelectWithLabel;
import views.components.ToastMessage;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

/** Renders a page for editing predicates of a block in a program. */
public class ProgramBlockPredicatesEditView extends BaseHtmlView {
  private static final String H2_CURRENT_VISIBILITY_CONDITION = "Current visibility condition";
  private static final String H2_NEW_VISIBILITY_CONDITION = "New visibility condition";
  private static final String TEXT_NO_VISIBILITY_CONDITIONS = "This screen is always shown.";
  private static final String TEXT_NO_AVAILABLE_QUESTIONS =
      "There are no available questions with which to set a visibility condition for this screen.";
  private static final String TEXT_NEW_VISIBILITY_CONDITION =
      "Apply a visibility condition using a question below. When you create a visibility"
          + " condition, it replaces the present one.";

  private final AdminLayout layout;

  @Inject
  public ProgramBlockPredicatesEditView(AdminLayout layout) {
    this.layout = checkNotNull(layout).setActivePage(NavPage.PROGRAMS);
  }

  public Content render(
      Http.Request request,
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      ImmutableList<QuestionDefinition> potentialPredicateQuestions) {

    String title = String.format("Visibility condition for %s", blockDefinition.name());
    Tag csrfTag = makeCsrfTokenInputTag(request);

    String predicateUpdateUrl =
        routes.AdminProgramBlockPredicatesController.update(
                programDefinition.id(), blockDefinition.id())
            .url();
    ImmutableList<Modal> modals =
        predicateFormModals(
            blockDefinition.name(), potentialPredicateQuestions, predicateUpdateUrl, csrfTag);

    String removePredicateUrl =
        routes.AdminProgramBlockPredicatesController.destroy(
                programDefinition.id(), blockDefinition.id())
            .url();
    String removePredicateFormId = "visibility-predicate-form-remove";
    Tag removePredicateForm =
        form(csrfTag)
            .withId(removePredicateFormId)
            .withMethod(POST)
            .withAction(removePredicateUrl)
            .with(
                submitButton("Remove visibility condition")
                    .attr(Attr.FORM, removePredicateFormId)
                    .attr(blockDefinition.visibilityPredicate().isEmpty() ? Attr.DISABLED : ""));

    String editBlockUrl =
        routes.AdminProgramBlocksController.edit(programDefinition.id(), blockDefinition.id())
            .url();

    ContainerTag content =
        div()
            .withClasses(Styles.MX_6, Styles.MY_10, Styles.FLEX, Styles.FLEX_COL, Styles.GAP_6)
            .with(
                div()
                    .withClasses(Styles.FLEX, Styles.FLEX_ROW)
                    .with(h1(title).withClasses(Styles.FONT_BOLD, Styles.TEXT_XL))
                    .with(div().withClasses(Styles.FLEX_GROW))
                    .with(
                        new LinkElement()
                            .setHref(editBlockUrl)
                            .setText(String.format("Return to edit %s", blockDefinition.name()))
                            .asAnchorText()))
            .with(
                div()
                    .with(
                        h2(H2_CURRENT_VISIBILITY_CONDITION)
                            .withClasses(Styles.FONT_SEMIBOLD, Styles.TEXT_LG))
                    .with(
                        div(blockDefinition.visibilityPredicate().isPresent()
                                ? blockDefinition
                                    .visibilityPredicate()
                                    .get()
                                    .toDisplayString(
                                        blockDefinition.name(), potentialPredicateQuestions)
                                : TEXT_NO_VISIBILITY_CONDITIONS)
                            .withClasses(ReferenceClasses.PREDICATE_DISPLAY)))
            .with(removePredicateForm)
            .with(
                div()
                    .with(
                        h2(H2_NEW_VISIBILITY_CONDITION)
                            .withClasses(Styles.FONT_SEMIBOLD, Styles.TEXT_LG))
                    .with(div(TEXT_NEW_VISIBILITY_CONDITION).withClasses(Styles.MB_2))
                    .with(
                        modals.isEmpty()
                            ? text(TEXT_NO_AVAILABLE_QUESTIONS)
                            : renderPredicateModalTriggerButtons(modals)));

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(layout.renderProgramInfo(programDefinition), content);

    Http.Flash flash = request.flash();
    if (flash.get("error").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.error(flash.get("error").get()).setDuration(-1));
    } else if (flash.get("success").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(flash.get("success").get()).setDuration(-1));
    }

    for (Modal modal : modals) {
      htmlBundle = htmlBundle.addModals(modal);
    }

    return layout.renderCentered(htmlBundle);
  }

  private ImmutableList<Modal> predicateFormModals(
      String blockName,
      ImmutableList<QuestionDefinition> questionDefinitions,
      String predicateUpdateUrl,
      Tag csrfTag) {
    ImmutableList.Builder<Modal> builder = ImmutableList.builder();
    for (QuestionDefinition qd : questionDefinitions) {
      builder.add(predicateFormModal(blockName, qd, predicateUpdateUrl, csrfTag));
    }
    return builder.build();
  }

  private Modal predicateFormModal(
      String blockName,
      QuestionDefinition questionDefinition,
      String predicateUpdateUrl,
      Tag csrfTag) {
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
            .with(renderPredicateForm(blockName, questionDefinition, predicateUpdateUrl, csrfTag));

    return Modal.builder(
            String.format("predicate-modal-%s", questionDefinition.getId()), modalContent)
        .setModalTitle(String.format("Add a visibility condition for %s", blockName))
        .setTriggerButtonContent(triggerButtonContent)
        .setTriggerButtonStyles(AdminStyles.BUTTON_QUESTION_PREDICATE)
        .build();
  }

  private Tag renderPredicateForm(
      String blockName,
      QuestionDefinition questionDefinition,
      String predicateUpdateUrl,
      Tag csrfTag) {
    String formId = String.format("visibility-predicate-form-%s", questionDefinition.getId());

    return form(csrfTag)
        .withId(formId)
        .withMethod(POST)
        .withAction(predicateUpdateUrl)
        .with(createActionDropdown(blockName))
        .with(renderQuestionDefinitionBox(questionDefinition))
        // Need to pass in the question ID with the rest of the form data in order to save the
        // correct predicate. However, this field's value is already known and set by the time the
        // modal is open, so make this field hidden.
        .with(createHiddenQuestionDefinitionInput(questionDefinition))
        .with(
            div()
                .withClasses(
                    ReferenceClasses.PREDICATE_OPTIONS, Styles.FLEX, Styles.FLEX_ROW, Styles.GAP_1)
                .with(createScalarDropdown(questionDefinition))
                .with(createOperatorDropdown())
                .with(createValueField(questionDefinition)))
        .with(submitButton("Submit").attr(Attr.FORM, formId));
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
            Styles.MY_2,
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

  private ContainerTag createActionDropdown(String blockName) {
    ImmutableMap<String, String> actionOptions =
        Arrays.stream(PredicateAction.values())
            .collect(toImmutableMap(PredicateAction::toDisplayString, PredicateAction::name));
    return new SelectWithLabel()
        .setFieldName("predicateAction")
        .setLabelText(String.format("%s should be", blockName))
        .setOptions(actionOptions)
        .addReferenceClass(ReferenceClasses.PREDICATE_ACTION)
        .getContainer();
  }

  private Tag createHiddenQuestionDefinitionInput(QuestionDefinition questionDefinition) {
    return input()
        .withName("questionId")
        .withType("hidden")
        .withValue(String.valueOf(questionDefinition.getId()));
  }

  private ContainerTag createScalarDropdown(QuestionDefinition questionDefinition) {
    ImmutableSet<Scalar> scalars;
    try {
      scalars = Scalar.getScalars(questionDefinition.getQuestionType());
    } catch (InvalidQuestionTypeException | UnsupportedQuestionTypeException e) {
      // This should never happen since we filter out Enumerator questions before this point.
      return div()
          .withText("Sorry, you cannot create a show/hide predicate with this question type.");
    }

    ImmutableList<ContainerTag> options =
        scalars.stream()
            .map(
                scalar ->
                    option(scalar.toDisplayString())
                        .withValue(scalar.name())
                        // Add the scalar type as data so we can determine which operators to allow.
                        .withData("type", scalar.toScalarType().name().toLowerCase()))
            .collect(toImmutableList());

    return new SelectWithLabel()
        .setFieldName("scalar")
        .setLabelText("Field")
        .setCustomOptions(options)
        .addReferenceClass(ReferenceClasses.PREDICATE_SCALAR_SELECT)
        .getContainer();
  }

  private ContainerTag createOperatorDropdown() {
    ImmutableList<ContainerTag> operatorOptions =
        Arrays.stream(Operator.values())
            .map(
                operator -> {
                  // Add this operator's allowed scalar types as data, so that we can determine
                  // whether to show or hide each operator based on the current type of scalar
                  // selected.
                  ContainerTag option =
                      option(operator.toDisplayString()).withValue(operator.name());
                  operator
                      .getOperableTypes()
                      .forEach(type -> option.attr("data-" + type.name().toLowerCase()));
                  return option;
                })
            .collect(toImmutableList());

    return new SelectWithLabel()
        .setFieldName("operator")
        .setLabelText("Operator")
        .setCustomOptions(operatorOptions)
        .addReferenceClass(ReferenceClasses.PREDICATE_OPERATOR_SELECT)
        .getContainer();
  }

  private ContainerTag createValueField(QuestionDefinition questionDefinition) {
    if (questionDefinition.getQuestionType().isMultiOptionType()) {
      // If it's a multi-option question, we need to provide a discrete list of possible values to
      // choose from instead of a freeform text field. Not only is it a better UX, but we store the
      // ID of the options rather than the display strings since the option display strings are
      // localized.
      ImmutableList<QuestionOption> options =
          ((MultiOptionQuestionDefinition) questionDefinition).getOptions();
      ContainerTag valueOptionsDiv =
          div().with(div("Values").withClasses(BaseStyles.CHECKBOX_GROUP_LABEL));
      for (QuestionOption option : options) {
        ContainerTag optionCheckbox =
            FieldWithLabel.checkbox()
                .setFieldName("predicateValues[]")
                .setValue(String.valueOf(option.id()))
                .setLabelText(option.optionText().getDefault())
                .getContainer();
        valueOptionsDiv.with(optionCheckbox);
      }
      return valueOptionsDiv;
    } else {
      return div()
          .with(
              FieldWithLabel.input()
                  .setFieldName("predicateValue")
                  .setLabelText("Value")
                  .addReferenceClass(ReferenceClasses.PREDICATE_VALUE_INPUT)
                  .getContainer())
          .with(
              div()
                  .withClasses(
                      ReferenceClasses.PREDICATE_VALUE_COMMA_HELP_TEXT,
                      Styles.HIDDEN,
                      Styles.TEXT_XS,
                      BaseStyles.FORM_LABEL_TEXT_COLOR)
                  .withText("Enter a list of comma-separated values. For example, \"v1,v2,v3\"."));
    }
  }
}
