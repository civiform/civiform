package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
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
import com.google.common.collect.ImmutableSet;
import controllers.admin.routes;
import j2html.TagCreator;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.LabelTag;
import j2html.tags.specialized.OptionTag;
import java.util.Arrays;
import java.util.UUID;
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
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.SelectWithLabel;
import views.components.ToastMessage;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;

/** Renders a page for editing predicates of a block in a program. */
public final class ProgramBlockPredicatesEditView extends ProgramBlockView {

  private final AdminLayout layout;

  public enum TYPE {
    ELIGIBILITY,
    VISIBILITY
  }

  @Inject
  public ProgramBlockPredicatesEditView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
  }

  /**
   * Renders the Predicate editor.
   *
   * <p>The UI:
   *
   * <ul>
   *   <li>Shows the current predicate
   *   <li>Allows for removing (/destroy handler) the existing predicate.
   *   <li>Presents options to set (/update handler) a new predicate for each question in {@code
   *       predicateQuestions}.
   * </ul>
   *
   * <p>Only one predicate can exist on a block so the UI does a full replace operation when
   * setting/updating a predicate.
   */
  public Content render(
      Http.Request request,
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      ImmutableList<QuestionDefinition> predicateQuestions,
      TYPE type) {

    final String predicateTypeNameTitleCase;
    final String h2_current_condition;
    final String text_no_conditions;
    final String h2_new_condition;
    final String text_new_condition;
    final String text_no_available_questions;
    final String predicateUpdateUrl;
    final String removePredicateUrl;
    switch (type) {
      case ELIGIBILITY:
        predicateTypeNameTitleCase = "Eligibility";
        h2_current_condition = "Current visibility condition";
        text_no_conditions = "This screen is always shown.";
        h2_new_condition = "New visibility condition";
        text_new_condition =
            "Apply a visibility condition using a question below. When you create a visibility"
                + " condition, it replaces the present one.";
        text_no_available_questions =
            "There are no available questions with which to set a visibility condition for this"
                + " screen.";
        predicateUpdateUrl =
            routes.AdminProgramBlockPredicatesController.updateEligibility(
                    programDefinition.id(), blockDefinition.id())
                .url();
        removePredicateUrl =
            routes.AdminProgramBlockPredicatesController.destroyEligibility(
                    programDefinition.id(), blockDefinition.id())
                .url();
        break;
      case VISIBILITY:
        predicateTypeNameTitleCase = "Visibility";
        h2_current_condition = "Current visibility condition";
        text_no_conditions = "This screen is always shown.";
        h2_new_condition = "New visibility condition";
        text_new_condition =
            "Apply a visibility condition using a question below. When you create a visibility"
                + " condition, it replaces the present one.";
        text_no_available_questions =
            "There are no available questions with which to set a visibility condition for this"
                + " screen.";
        predicateUpdateUrl =
            routes.AdminProgramBlockPredicatesController.update(
                    programDefinition.id(), blockDefinition.id())
                .url();
        removePredicateUrl =
            routes.AdminProgramBlockPredicatesController.destroy(
                    programDefinition.id(), blockDefinition.id())
                .url();
        break;
      default:
        throw new IllegalArgumentException(
            String.format("Predicate type %s is unsupported.", type));
    }
    InputTag csrfTag = makeCsrfTokenInputTag(request);
    ImmutableList<Modal> modals =
        createPredicateUpdateFormModals(
            blockDefinition.name(), predicateQuestions, predicateUpdateUrl, csrfTag);

    String title =
        String.format("%s condition for %s", predicateTypeNameTitleCase, blockDefinition.name());
    String removePredicateFormId = UUID.randomUUID().toString();
    FormTag removePredicateForm =
        form(csrfTag)
            .withId(removePredicateFormId)
            .withMethod(POST)
            .withAction(removePredicateUrl)
            .with(
                submitButton(
                        String.format(
                            "Remove %s condition", predicateTypeNameTitleCase.toLowerCase()))
                    .withForm(removePredicateFormId)
                    .withCondDisabled(blockDefinition.visibilityPredicate().isEmpty()));

    // Link back to the block editor.
    String editBlockUrl =
        routes.AdminProgramBlocksController.edit(programDefinition.id(), blockDefinition.id())
            .url();

    DivTag content =
        div()
            .withClasses("mx-6", "my-10", "flex", "flex-col", "gap-6")
            // Link back to the editor for this predicate's block.
            .with(
                div()
                    .withClasses("flex", "flex-row")
                    .with(h1(title).withClasses("font-bold", "text-xl"))
                    .with(div().withClasses("flex-grow"))
                    .with(
                        new LinkElement()
                            .setHref(editBlockUrl)
                            .setText(String.format("Return to edit %s", blockDefinition.name()))
                            .asAnchorText()))
            // Show the current predicate.
            .with(
                div()
                    .with(h2(h2_current_condition).withClasses("font-semibold", "text-lg"))
                    .with(
                        div(blockDefinition
                                .visibilityPredicate()
                                .map(
                                    pred ->
                                        pred.toDisplayString(
                                            blockDefinition.name(), predicateQuestions))
                                .orElse(text_no_conditions))
                            .withClasses(ReferenceClasses.PREDICATE_DISPLAY)))
            // Show the control to remove the current predicate.
            .with(removePredicateForm)
            // Show all available questions that predicates can be made for, for this block.
            .with(
                div()
                    .with(h2(h2_new_condition).withClasses("font-semibold", "text-lg"))
                    .with(div(text_new_condition).withClasses("mb-2"))
                    .with(
                        modals.isEmpty()
                            ? text(text_no_available_questions)
                            : renderPredicateModalTriggerButtons(modals)));

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(renderProgramInfo(programDefinition), content);

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

  private ImmutableList<Modal> createPredicateUpdateFormModals(
      String blockName,
      ImmutableList<QuestionDefinition> questionDefinitions,
      String predicateUpdateUrl,
      InputTag csrfTag) {
    ImmutableList.Builder<Modal> builder = ImmutableList.builder();
    for (QuestionDefinition qd : questionDefinitions) {
      builder.add(
          createQuestionViewAndPredicateUpdateFormModal(
              blockName, qd, predicateUpdateUrl, csrfTag));
    }
    return builder.build();
  }

  /**
   * Creates a display of the {@code questionDefinition} with a button that presents a predicate
   * creator for the question.
   *
   * <p>The predicate editor will POST to {@code predicateUpdateUrl} upon submit.
   */
  private Modal createQuestionViewAndPredicateUpdateFormModal(
      String blockName,
      QuestionDefinition questionDefinition,
      String predicateUpdateUrl,
      InputTag csrfTag) {
    String questionHelpText =
        questionDefinition.getQuestionHelpText().isEmpty()
            ? ""
            : questionDefinition.getQuestionHelpText().getDefault();
    ButtonTag triggerButtonContent =
        TagCreator.button()
            .with(
                div()
                    .withClasses("flex", "flex-row", "gap-4")
                    .with(
                        Icons.questionTypeSvg(questionDefinition.getQuestionType())
                            .withClasses("shrink-0", "h-12", "w-6"))
                    .with(
                        div()
                            .withClasses("text-left")
                            .with(
                                div(questionDefinition.getQuestionText().getDefault()),
                                div(questionHelpText).withClasses("mt-1", "text-sm"),
                                div(String.format("Admin ID: %s", questionDefinition.getName()))
                                    .withClasses("mt-1", "text-sm"))));

    DivTag modalContent =
        div()
            .withClasses("m-4")
            .with(
                renderPredicateUpdateForm(
                    blockName, questionDefinition, predicateUpdateUrl, csrfTag));

    return Modal.builder(
            String.format("predicate-modal-%s", questionDefinition.getId()), modalContent)
        .setModalTitle(String.format("Add a visibility condition for %s", blockName))
        .setTriggerButtonContent(triggerButtonContent)
        .setTriggerButtonStyles(AdminStyles.BUTTON_QUESTION_PREDICATE)
        .build();
  }

  /**
   * Renders a form to configure a predicate for {@code questionDefinition} and submit it to {@code
   * predicateUpdateUrl}.
   */
  private FormTag renderPredicateUpdateForm(
      String blockName,
      QuestionDefinition questionDefinition,
      String predicateUpdateUrl,
      InputTag csrfTag) {
    String formId = UUID.randomUUID().toString();

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
                .withClasses(ReferenceClasses.PREDICATE_OPTIONS, "flex", "flex-row", "gap-1")
                .with(createScalarDropdown(questionDefinition))
                .with(createOperatorDropdown())
                .with(createValueField(questionDefinition)))
        .with(submitButton("Submit").withForm(formId));
  }

  private DivTag renderPredicateModalTriggerButtons(ImmutableList<Modal> modals) {
    return div().withClasses("flex", "flex-col", "gap-2").with(each(modals, Modal::getButton));
  }

  private DivTag renderQuestionDefinitionBox(QuestionDefinition questionDefinition) {
    String questionHelpText =
        questionDefinition.getQuestionHelpText().isEmpty()
            ? ""
            : questionDefinition.getQuestionHelpText().getDefault();
    return div()
        .withClasses(
            "flex", "flex-row", "gap-4", "px-4", "py-2", "my-2", "border", "border-gray-200")
        .with(
            Icons.questionTypeSvg(questionDefinition.getQuestionType())
                .withClasses("shrink-0", "h-12", "w-6"))
        .with(
            div()
                .with(
                    div(questionDefinition.getQuestionText().getDefault()),
                    div(questionHelpText).withClasses("mt-1", "text-sm"),
                    div(String.format("Admin ID: %s", questionDefinition.getName()))
                        .withClasses("mt-1", "text-sm")));
  }

  private DivTag createActionDropdown(String blockName) {
    ImmutableList<SelectWithLabel.OptionValue> actionOptions =
        Arrays.stream(PredicateAction.values())
            .map(
                action ->
                    SelectWithLabel.OptionValue.builder()
                        .setLabel(action.toDisplayString())
                        .setValue(action.name())
                        .build())
            .collect(ImmutableList.toImmutableList());
    return new SelectWithLabel()
        .setFieldName("predicateAction")
        .setLabelText(String.format("%s should be", blockName))
        .setOptions(actionOptions)
        .addReferenceClass(ReferenceClasses.PREDICATE_ACTION)
        .getSelectTag();
  }

  private InputTag createHiddenQuestionDefinitionInput(QuestionDefinition questionDefinition) {
    return input()
        .withName("questionId")
        .withType("hidden")
        .withValue(String.valueOf(questionDefinition.getId()));
  }

  private DivTag createScalarDropdown(QuestionDefinition questionDefinition) {
    ImmutableSet<Scalar> scalars;
    try {
      scalars = Scalar.getScalars(questionDefinition.getQuestionType());
    } catch (InvalidQuestionTypeException | UnsupportedQuestionTypeException e) {
      // This should never happen since we filter out Enumerator questions before this point.
      return div()
          .withText("Sorry, you cannot create a show/hide predicate with this question type.");
    }

    ImmutableList<OptionTag> options =
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
        .getSelectTag();
  }

  private DivTag createOperatorDropdown() {
    ImmutableList<OptionTag> operatorOptions =
        Arrays.stream(Operator.values())
            .map(
                operator -> {
                  // Add this operator's allowed scalar types as data, so that we can determine
                  // whether to show or hide each operator based on the current type of scalar
                  // selected.
                  OptionTag option = option(operator.toDisplayString()).withValue(operator.name());
                  operator
                      .getOperableTypes()
                      .forEach(type -> option.withData(type.name().toLowerCase(), ""));
                  return option;
                })
            .collect(toImmutableList());

    return new SelectWithLabel()
        .setFieldName("operator")
        .setLabelText("Operator")
        .setCustomOptions(operatorOptions)
        .addReferenceClass(ReferenceClasses.PREDICATE_OPERATOR_SELECT)
        .getSelectTag();
  }

  private DivTag createValueField(QuestionDefinition questionDefinition) {
    if (questionDefinition.getQuestionType().isMultiOptionType()) {
      // If it's a multi-option question, we need to provide a discrete list of possible values to
      // choose from instead of a freeform text field. Not only is it a better UX, but we store the
      // ID of the options rather than the display strings since the option display strings are
      // localized.
      ImmutableList<QuestionOption> options =
          ((MultiOptionQuestionDefinition) questionDefinition).getOptions();
      DivTag valueOptionsDiv =
          div().with(div("Values").withClasses(BaseStyles.CHECKBOX_GROUP_LABEL));
      for (QuestionOption option : options) {
        LabelTag optionCheckbox =
            FieldWithLabel.checkbox()
                .setFieldName("predicateValues[]")
                .setValue(String.valueOf(option.id()))
                .setLabelText(option.optionText().getDefault())
                .getCheckboxTag();
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
                  .getInputTag())
          .with(
              div()
                  .withClasses(
                      ReferenceClasses.PREDICATE_VALUE_COMMA_HELP_TEXT,
                      "hidden",
                      "text-xs",
                      BaseStyles.FORM_LABEL_TEXT_COLOR)
                  .withText("Enter a list of comma-separated values. For example, \"v1,v2,v3\"."));
    }
  }
}
