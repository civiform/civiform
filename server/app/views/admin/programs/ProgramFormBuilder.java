package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.iff;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import controllers.applicant.routes;
import forms.ProgramForm;
import j2html.tags.DomContent;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.LabelTag;
import java.util.List;
import java.util.Map;
import models.CategoryModel;
import models.DisplayMode;
import models.ProgramNotificationPreference;
import models.TrustedIntermediaryGroupModel;
import modules.MainModule;
import play.mvc.Http.Request;
import repository.AccountRepository;
import repository.CategoryRepository;
import services.Path;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.settings.SettingsManifest;
import views.BaseHtmlView;
import views.ViewUtils;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.Modal;
import views.components.Modal.Width;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/**
 * Builds a program form for rendering. If the program was previously created, the {@code adminName}
 * field is disabled, since it cannot be edited once set.
 */
abstract class ProgramFormBuilder extends BaseHtmlView {
  private static final String ELIGIBILITY_IS_GATING_FIELD_NAME = "eligibilityIsGating";

  private final SettingsManifest settingsManifest;
  private final String baseUrl;
  private final AccountRepository accountRepository;
  private final CategoryRepository categoryRepository;

  ProgramFormBuilder(
      Config configuration,
      SettingsManifest settingsManifest,
      AccountRepository accountRepository,
      CategoryRepository categoryRepository) {
    this.settingsManifest = settingsManifest;
    this.baseUrl = checkNotNull(configuration).getString("base_url");
    this.accountRepository = checkNotNull(accountRepository);
    this.categoryRepository = checkNotNull(categoryRepository);
  }

  /** Builds the form using program form data. */
  protected final FormTag buildProgramForm(
      Request request, ProgramForm program, ProgramEditStatus programEditStatus) {
    return buildProgramForm(
        request,
        program.getAdminName(),
        program.getAdminDescription(),
        program.getLocalizedDisplayName(),
        program.getLocalizedDisplayDescription(),
        program.getLocalizedShortDescription(),
        program.getExternalLink(),
        program.getLocalizedConfirmationMessage(),
        program.getDisplayMode(),
        ImmutableList.copyOf(program.getNotificationPreferences()),
        program.getEligibilityIsGating(),
        program.getIsCommonIntakeForm(),
        programEditStatus,
        ImmutableSet.copyOf(program.getTiGroups()),
        ImmutableList.copyOf(program.getCategories()),
        ImmutableList.copyOf(program.getApplicationSteps()));
  }

  /** Builds the form using program definition data. */
  protected final FormTag buildProgramForm(
      Request request, ProgramDefinition program, ProgramEditStatus programEditStatus) {
    return buildProgramForm(
        request,
        program.adminName(),
        program.adminDescription(),
        program.localizedName().getDefault(),
        program.localizedDescription().getDefault(),
        program.localizedShortDescription().getDefault(),
        program.externalLink(),
        program.localizedConfirmationMessage().getDefault(),
        program.displayMode().getValue(),
        program.notificationPreferences().stream()
            .map(ProgramNotificationPreference::getValue)
            .collect(ImmutableList.toImmutableList()),
        program.eligibilityIsGating(),
        program.programType().equals(ProgramType.COMMON_INTAKE_FORM),
        programEditStatus,
        program.acls().getTiProgramViewAcls(),
        program.categories().stream()
            .map(CategoryModel::getId)
            .collect(ImmutableList.toImmutableList()),
        program.applicationSteps().stream()
            .map(
                step ->
                    Map.of(
                        "title",
                        step.getTitle().getDefault(),
                        "description",
                        step.getDescription().getDefault()))
            .collect(ImmutableList.toImmutableList()));
  }

  private FormTag buildProgramForm(
      Request request,
      String adminName,
      String adminDescription,
      String displayName,
      String displayDescription,
      String shortDescription,
      String externalLink,
      String confirmationSceen,
      String displayMode,
      ImmutableList<String> notificationPreferences,
      boolean eligibilityIsGating,
      Boolean isCommonIntakeForm,
      ProgramEditStatus programEditStatus,
      ImmutableSet<Long> selectedTi,
      ImmutableList<Long> categories,
      ImmutableList<Map<String, String>> applicationSteps) {
    System.out.println(applicationSteps);
    List<CategoryModel> categoryOptions = categoryRepository.listCategories();
    FormTag formTag = form().withMethod("POST").withId("program-details-form");
    formTag.with(
        requiredFieldsExplanationContent(),
        h2("Program setup").withClasses("py-2", "mt-6", "font-semibold"),
        FieldWithLabel.input()
            .setId("program-display-name-input")
            .setFieldName("localizedDisplayName")
            .setLabelText("Program name")
            .setRequired(true)
            .setValue(displayName)
            .getInputTag(),
        FieldWithLabel.textArea()
            .setId("program-display-short-description-textarea")
            .setFieldName("localizedShortDescription")
            .setLabelText(
                "Short description of this program for the public. Maximum 100 characters.")
            .setMaxLength(100)
            .setRequired(true)
            .setMarkdownSupported(true)
            .setValue(shortDescription)
            .getTextareaTag(),
        programUrlField(adminName, programEditStatus),
        FieldWithLabel.textArea()
            .setId("program-description-textarea")
            .setFieldName("adminDescription")
            .setLabelText("Program note for administrative use only (optional)")
            .setValue(adminDescription)
            .getTextareaTag(),
        FieldWithLabel.checkbox()
            .setId("common-intake-checkbox")
            .setFieldName("isCommonIntakeForm")
            .setLabelText("Set program as pre-screener")
            .addStyleClass("border-none")
            .setValue("true")
            .setChecked(isCommonIntakeForm)
            .getCheckboxTag()
            .with(
                span(ViewUtils.makeSvgToolTip(
                        "You can set one program as the ‘pre-screener’. This will pin the"
                            + " program card to the top of the programs and services page"
                            + " while moving other program cards below it.",
                        Icons.INFO))
                    .withClass("ml-2")),
        // Hidden checkbox used to signal whether or not the user has confirmed they want to
        // change which program is marked as the common intake form.
        FieldWithLabel.checkbox()
            .setId("confirmed-change-common-intake-checkbox")
            .setFieldName("confirmedChangeCommonIntakeForm")
            .setValue("false")
            .setChecked(false)
            .addStyleClass("hidden")
            .getCheckboxTag(),
        fieldset()
            .with(
                legend("Program eligibility gating")
                    .withClass(BaseStyles.INPUT_LABEL)
                    .with(ViewUtils.requiredQuestionIndicator())
                    .with(p("(Not applicable if this program is the pre-screener)")),
                FieldWithLabel.radio()
                    .setFieldName(ELIGIBILITY_IS_GATING_FIELD_NAME)
                    .setAriaRequired(true)
                    .setLabelText(
                        "Only allow residents to submit applications if they meet all eligibility"
                            + " requirements")
                    .setValue(String.valueOf(true))
                    .setChecked(eligibilityIsGating)
                    .getRadioTag(),
                FieldWithLabel.radio()
                    .setFieldName(ELIGIBILITY_IS_GATING_FIELD_NAME)
                    .setAriaRequired(true)
                    .setLabelText(
                        "Allow residents to submit applications even if they don't meet eligibility"
                            + " requirements")
                    .setValue(String.valueOf(false))
                    .setChecked(!eligibilityIsGating)
                    .getRadioTag()),
        iff(
            settingsManifest.getProgramFilteringEnabled(request) && !categoryOptions.isEmpty(),
            showCategoryCheckboxes(categoryOptions, categories, isCommonIntakeForm)),
        fieldset()
            .with(
                legend("Program visibility")
                    .withClass(BaseStyles.INPUT_LABEL)
                    .with(ViewUtils.requiredQuestionIndicator()),
                FieldWithLabel.radio()
                    .setId("program-display-mode-public")
                    .setFieldName("displayMode")
                    .setAriaRequired(true)
                    .setLabelText("Publicly visible")
                    .setValue(DisplayMode.PUBLIC.getValue())
                    .setChecked(displayMode.equals(DisplayMode.PUBLIC.getValue()))
                    .getRadioTag(),
                FieldWithLabel.radio()
                    .setId("program-display-mode-hidden")
                    .setFieldName("displayMode")
                    .setAriaRequired(true)
                    .setLabelText(
                        "Hide from applicants. Only individuals with the unique program link can"
                            + " access this program")
                    .setValue(DisplayMode.HIDDEN_IN_INDEX.getValue())
                    .setChecked(displayMode.equals(DisplayMode.HIDDEN_IN_INDEX.getValue()))
                    .getRadioTag(),
                FieldWithLabel.radio()
                    .setId("program-display-mode-ti-only")
                    .setFieldName("displayMode")
                    .setAriaRequired(true)
                    .setLabelText("Trusted intermediaries only")
                    .setValue(DisplayMode.TI_ONLY.getValue())
                    .setChecked(displayMode.equals(DisplayMode.TI_ONLY.getValue()))
                    .getRadioTag(),
                FieldWithLabel.radio()
                    .setId("program-display-mode-select-ti-only")
                    .setFieldName("displayMode")
                    .setAriaRequired(true)
                    .setLabelText("Visible to selected trusted intermediaries only")
                    .setValue(DisplayMode.SELECT_TI.getValue())
                    .setChecked(displayMode.equals(DisplayMode.SELECT_TI.getValue()))
                    .getRadioTag(),
                showTiSelectionList(
                    selectedTi, displayMode.equals(DisplayMode.SELECT_TI.getValue())))
            .condWith(
                settingsManifest.getDisabledVisibilityConditionEnabled(request),
                FieldWithLabel.radio()
                    .setId("program-display-mode-disabled")
                    .setFieldName("displayMode")
                    .setAriaRequired(true)
                    .setLabelText("Disabled")
                    .setValue(DisplayMode.DISABLED.getValue())
                    .setChecked(displayMode.equals(DisplayMode.DISABLED.getValue()))
                    .getRadioTag()),
        fieldset()
            .with(
                legend("Email notifications").withClass(BaseStyles.INPUT_LABEL),
                FieldWithLabel.checkbox()
                    .setFieldName("notificationPreferences")
                    .setAriaRequired(true)
                    .setLabelText(
                        "Send Program Admins an email notification every time an application is"
                            + " submitted")
                    .setValue(
                        ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS
                            .getValue())
                    .setChecked(
                        notificationPreferences.contains(
                            ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS
                                .getValue()))
                    .getCheckboxTag()),
        h2("Program overview").withClasses("py-2", "mt-6", "font-semibold"),
        FieldWithLabel.textArea()
            .setId("program-display-description-textarea")
            .setFieldName("localizedDisplayDescription")
            .setLabelText("Long program description (optional)")
            .setMarkdownSupported(true)
            .setValue(displayDescription)
            .getTextareaTag(),
        FieldWithLabel.input()
            .setId("program-external-link-input")
            .setFieldName("externalLink")
            .setLabelText("Link to program website (optional)")
            .setValue(externalLink)
            .getInputTag(),
        h2("How to apply").withClasses("py-2", "mt-6", "font-semibold"),
        div().withId("apply-steps").with(buildApplicationSteps(applicationSteps)),
        h2("Confirmation message").withClasses("py-2", "mt-6", "font-semibold"),
        FieldWithLabel.textArea()
            .setId("program-confirmation-message-textarea")
            .setFieldName("localizedConfirmationMessage")
            .setLabelText(
                "A custom message that will be shown on the confirmation page after an application"
                    + " has been submitted. You can use this message to explain next steps of the"
                    + " application process and/or highlight other programs to apply for."
                    + " (optional)")
            .setMarkdownSupported(true)
            .setValue(confirmationSceen)
            .getTextareaTag());

    formTag.with(createSubmitButton(programEditStatus));
    return formTag;
  }

  private DivTag buildApplicationSteps(ImmutableList<Map<String, String>> applicationSteps) {

    DivTag div = div();
    // build 5 application steps
    for (int i = 0; i < 5; i++) {
      String title = "";
      String description = "";
      if (i + 1 <= applicationSteps.size()) {
        title = applicationSteps.get(i).get("title");
        description = applicationSteps.get(i).get("description");
      }
      div.with(buildApplicationStepDiv(i, title, description));
    }

    return div;
  }

  private DivTag buildApplicationStepDiv(int i, String titleValue, String descriptionValue) {

    String index = Integer.toString(i);
    String indexPlusOne = Integer.toString(i + 1);
    FieldWithLabel title =
        FieldWithLabel.input()
            .setId("apply-step-" + indexPlusOne + "-title")
            .setFieldName("applicationSteps[" + index + "][title]")
            .setValue(titleValue);

    FieldWithLabel description =
        FieldWithLabel.textArea()
            .setId("apply-step-" + indexPlusOne + "-description")
            .setFieldName("applicationSteps[" + index + "][description]")
            .setMarkdownSupported(true)
            .setValue(descriptionValue);

    if (indexPlusOne.equals("1")) {
      title.setLabelText("Step " + indexPlusOne + " title").setRequired(true);
      description.setLabelText("Step " + indexPlusOne + " description").setRequired(true);
    } else {
      title.setLabelText("Step " + indexPlusOne + " title (optional)");
      description.setLabelText("Step " + indexPlusOne + " description (optional)");
    }

    return div()
        .withId("apply-step-" + indexPlusOne + "-div")
        .with(title.getInputTag(), description.getTextareaTag());
  }

  private DivTag showCategoryCheckboxes(
      List<CategoryModel> categoryOptions, List<Long> categories, boolean isCommonIntakeForm) {
    return div(
            legend(
                    "Tag this program with 1 or more categories to make it easier to find"
                        + " (optional)")
                .withClass("text-gray-600"),
            fieldset(
                    div(each(
                            categoryOptions,
                            category ->
                                div(
                                        input()
                                            .withClasses(
                                                "usa-checkbox__input usa-checkbox__input--tile")
                                            .withId(
                                                "checkbox-category-" + category.getDefaultName())
                                            .withType("checkbox")
                                            .withName("categories" + Path.ARRAY_SUFFIX)
                                            .withValue(String.valueOf(category.getId()))
                                            .withCondDisabled(isCommonIntakeForm)
                                            .withCondChecked(
                                                categories.contains(category.getId())
                                                    && !isCommonIntakeForm),
                                        label(category.getDefaultName())
                                            .withClasses("usa-checkbox__label")
                                            .withFor(
                                                "checkbox-category-" + category.getDefaultName()))
                                    .withClasses(
                                        "usa-checkbox", "grid-col-12", "tablet:grid-col-6")))
                        .withClasses("grid-row", "grid-gap-md"))
                .withId("category-checkboxes")
                .withClasses("usa-fieldset"))
        .withClasses("mb-2");
  }

  private DomContent showTiSelectionList(ImmutableSet<Long> selectedTi, boolean selectTiChecked) {
    List<TrustedIntermediaryGroupModel> tiGroups =
        accountRepository.listTrustedIntermediaryGroups();
    DivTag tiSelectionRenderer =
        div()
            // Hidden input that's always selected to allow for clearing multi-select data.
            .with(
                input()
                    .withType("checkbox")
                    .withName("tiGroups" + Path.ARRAY_SUFFIX)
                    .withValue("")
                    .withCondChecked(true)
                    .withClasses(ReferenceClasses.RADIO_DEFAULT, "hidden"))
            .with(
                tiGroups.stream()
                    .map(
                        option ->
                            renderCheckboxOption(
                                option.getName(), option.id, selectedTi.contains(option.id))));
    DivTag returnDivTag = div().withClasses("px-4 py-2").withId("TiList").with(tiSelectionRenderer);

    return selectTiChecked ? returnDivTag : returnDivTag.isHidden();
  }

  private DivTag renderCheckboxOption(String tiName, Long tiId, boolean selected) {
    String id = tiId.toString();
    LabelTag labelTag =
        label()
            .withClasses(
                ReferenceClasses.RADIO_OPTION,
                BaseStyles.CHECKBOX_LABEL,
                BaseStyles.BORDER_CIVIFORM_BLUE)
            .with(
                input()
                    .withId(id)
                    .withType("checkbox")
                    .withName("tiGroups" + Path.ARRAY_SUFFIX)
                    .withValue(String.valueOf(tiId))
                    .withCondChecked(selected)
                    .withClasses(
                        StyleUtils.joinStyles(ReferenceClasses.RADIO_INPUT, BaseStyles.CHECKBOX)),
                span(tiName).withClasses(ReferenceClasses.MULTI_OPTION_VALUE));

    return div()
        .withClasses(ReferenceClasses.MULTI_OPTION_QUESTION_OPTION, "my-2", "relative")
        .with(labelTag);
  }

  private DomContent programUrlField(String adminName, ProgramEditStatus programEditStatus) {
    if (programEditStatus != ProgramEditStatus.CREATION) {
      // Only allow editing the program URL at program creation time.
      String programUrl =
          baseUrl
              + routes.ApplicantProgramsController.show(MainModule.SLUGIFIER.slugify(adminName))
                  .url();
      return div()
          .withClass("mb-2")
          .with(
              p("The URL for this program. This value can't be changed")
                  .withClasses(BaseStyles.INPUT_LABEL),
              p(programUrl).withClasses(BaseStyles.FORM_FIELD));
    }
    return FieldWithLabel.input()
        .setId("program-name-input")
        .setFieldName("adminName")
        .setLabelText(
            "Enter an identifier that will be used in this program's applicant-facing URL. This"
                + " value can't be changed later. Aim to keep it short so it's easy to share. Use"
                + " a dash between each word")
        .setRequired(true)
        .setValue(adminName)
        .getInputTag();
  }

  protected Modal buildConfirmCommonIntakeChangeModal(String existingCommonIntakeFormDisplayName) {
    DivTag content =
        div()
            .withClasses("flex-row", "space-y-6")
            .with(
                p("The pre-screener will be updated from ")
                    .with(span(existingCommonIntakeFormDisplayName).withClass("font-bold"))
                    .withText(" to the current program."))
            .with(p("Would you like to confirm the change?"))
            .with(
                div()
                    .withClasses("flex")
                    .with(div().withClasses("flex-grow"))
                    .with(
                        submitButton("Confirm")
                            .withForm("program-details-form")
                            .withId("confirm-common-intake-change-button")
                            .withClasses(ButtonStyles.SOLID_BLUE, "cursor-pointer")));
    return Modal.builder()
        .setModalId("confirm-common-intake-change")
        .setLocation(Modal.Location.ADMIN_FACING)
        .setContent(content)
        .setModalTitle("Confirm pre-screener change?")
        .setDisplayOnLoad(true)
        .setWidth(Width.THIRD)
        .build();
  }

  private ButtonTag createSubmitButton(ProgramEditStatus programEditStatus) {
    String saveProgramDetailsText;
    if (programEditStatus == ProgramEditStatus.CREATION
        || programEditStatus == ProgramEditStatus.CREATION_EDIT) {
      // If the admin is in the middle of creating a new program, they'll be redirected to the next
      // step of adding a program image, so we want the save button text to reflect that.
      saveProgramDetailsText = "Save and continue to next step";
    } else {
      // If the admin is editing an existing program, they'll be redirected back to the program
      // blocks page.
      saveProgramDetailsText = "Save";
    }

    return submitButton(saveProgramDetailsText)
        .withId("program-update-button")
        .withClasses(ButtonStyles.SOLID_BLUE, "mt-6");
  }
}
