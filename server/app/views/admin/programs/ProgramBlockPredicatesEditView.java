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
import static views.ViewUtils.ProgramDisplayType.DRAFT;

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
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import play.mvc.Http;
import play.twirl.api.Content;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.question.QuestionOption;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.HtmlBundle;
import views.ViewUtils.ProgramDisplayType;
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
public final class ProgramBlockPredicatesEditView extends ProgramBlockBaseView {

  private final AdminLayout layout;

  // The functionality type of the predicate editor.
  public enum ViewType {
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
      ViewType viewType) {
    String blockName = blockDefinition.name();

    // This render code is used to render eligibility and visibility predicate editors.
    // The following vars set the per-type visual and url values and the rest lays things out
    // identically for the most part.

    final Optional<PredicateDefinition> predicateDef;
    final String modalTitle;
    final String predicateTypeNameTitleCase;
    final String h2CurrentCondition;
    final String textNoConditions;
    final String h2NewCondition;
    final String textNewCondition;
    final String textNoAvailableQuestions;
    final String predicateUpdateUrl;
    final String removePredicateUrl;
    switch (viewType) {
      case ELIGIBILITY:
        predicateDef =
            blockDefinition.eligibilityDefinition().map(EligibilityDefinition::predicate);

        modalTitle = String.format("Add an eligibility condition for %s", blockName);
        predicateTypeNameTitleCase = "Eligibility";
        h2CurrentCondition = "Current eligibility condition";
        textNoConditions = "This screen is always eligible.";
        h2NewCondition = "New eligibility condition";
        textNewCondition =
            "Apply a eligibility condition using a question below. When you create a eligibility"
                + " condition, it replaces the present one.";
        textNoAvailableQuestions =
            "There are no available questions with which to set an eligibility condition for this"
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
        predicateDef = blockDefinition.visibilityPredicate();
        modalTitle = String.format("Add a visibility condition for %s", blockName);
        predicateTypeNameTitleCase = "Visibility";
        h2CurrentCondition = "Current visibility condition";
        textNoConditions = "This screen is always shown.";
        h2NewCondition = "New visibility condition";
        textNewCondition =
            "Apply a visibility condition using a question below. When you create a visibility"
                + " condition, it replaces the present one.";
        textNoAvailableQuestions =
            "There are no available questions with which to set a visibility condition for this"
                + " screen.";
        predicateUpdateUrl =
            routes.AdminProgramBlockPredicatesController.updateVisibility(
                    programDefinition.id(), blockDefinition.id())
                .url();
        removePredicateUrl =
            routes.AdminProgramBlockPredicatesController.destroyVisibility(
                    programDefinition.id(), blockDefinition.id())
                .url();
        break;
      default:
        throw new IllegalArgumentException(
            String.format("Predicate type %s is unsupported.", viewType));
    }
    InputTag csrfTag = makeCsrfTokenInputTag(request);
    ImmutableList<Modal> modals =
        createPredicateUpdateFormModals(
            blockName, predicateQuestions, predicateUpdateUrl, modalTitle, viewType, csrfTag);

    String title = String.format("%s condition for %s", predicateTypeNameTitleCase, blockName);
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
                    .withCondDisabled(predicateDef.isEmpty()));

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
                            .setText(String.format("Return to edit %s", blockName))
                            .asAnchorText()))
            // Show the current predicate.
            .with(
                div()
                    .with(h2(h2CurrentCondition).withClasses("font-semibold", "text-lg"))
                    .with(
                        div(predicateDef
                                .map(pred -> pred.toDisplayString(blockName, predicateQuestions))
                                .orElse(textNoConditions))
                            .withClasses(ReferenceClasses.PREDICATE_DISPLAY)))
            // Show the control to remove the current predicate.
            .with(removePredicateForm)
            // Show all available questions that predicates can be made for, for this block.
            .with(
                div()
                    .with(h2(h2NewCondition).withClasses("font-semibold", "text-lg"))
                    .with(div(textNewCondition).withClasses("mb-2"))
                    .with(
                        modals.isEmpty()
                            ? text(textNoAvailableQuestions)
                            : renderPredicateModalTriggerButtons(modals)));

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(
                renderProgramInfo(programDefinition)
                    .with(renderEditProgramDetailsButton(programDefinition)),
                content);
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
      String modalTitle,
      ViewType viewType,
      InputTag csrfTag) {
    ImmutableList.Builder<Modal> builder = ImmutableList.builder();
    for (QuestionDefinition qd : questionDefinitions) {
      builder.add(
          createQuestionViewAndPredicateUpdateFormModal(
              blockName, qd, predicateUpdateUrl, modalTitle, viewType, csrfTag));
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
      String modalTitle,
      ViewType viewType,
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
                    blockName, questionDefinition, predicateUpdateUrl, viewType, csrfTag));

    return Modal.builder(
            String.format("predicate-modal-%s", questionDefinition.getId()), modalContent)
        .setModalTitle(modalTitle)
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
      ViewType viewType,
      InputTag csrfTag) {
    String formId = UUID.randomUUID().toString();

    var updateForm = form(csrfTag).withId(formId).withMethod(POST).withAction(predicateUpdateUrl);

    if (viewType.equals(ViewType.ELIGIBILITY)) {
      updateForm.with(createEligibilityHiddenAction());
    } else if (viewType.equals(ViewType.VISIBILITY)) {
      updateForm.with(createVisibilityActionDropdown(blockName));
    }

    return updateForm
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

  private InputTag createEligibilityHiddenAction() {
    return input()
        .withName("predicateAction")
        .withType("hidden")
        .withValue(PredicateAction.ELIGIBLE_BLOCK.name());
  }

  private DivTag createVisibilityActionDropdown(String blockName) {
    var actions = ImmutableList.of(PredicateAction.HIDE_BLOCK, PredicateAction.SHOW_BLOCK);
    ImmutableList<SelectWithLabel.OptionValue> actionOptions =
        actions.stream()
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
      // The old predicate creation endpoint does not support service areas
      scalars =
          Scalar.getScalars(questionDefinition.getQuestionType()).stream()
              .filter(scalar -> !scalar.equals(Scalar.SERVICE_AREA))
              .collect(ImmutableSet.toImmutableSet());
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
            // The old predicate creation endpoint does not support service areas
            .filter(operator -> !operator.equals(Operator.IN_SERVICE_AREA))
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

  private ButtonTag renderEditProgramDetailsButton(ProgramDefinition programDefinition) {
    ButtonTag editButton = getStandardizedEditButton("Edit program details");
    String editLink = routes.AdminProgramController.edit(programDefinition.id()).url();
    return asRedirectElement(editButton, editLink);
  }

  @Override
  protected ProgramDisplayType getProgramDisplayStatus() {
    return DRAFT;
  }
}
