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
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.FormTag;
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
import services.AlertType;
import services.Path;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.settings.SettingsManifest;
import views.AlertComponent;
import views.BaseHtmlView;
import views.ViewUtils;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.Modal;
import views.components.Modal.Width;
import views.style.BaseStyles;

/**
 * Builds a program form for rendering. If the program was previously created, the {@code adminName}
 * field is disabled, since it cannot be edited once set.
 */
abstract class ProgramFormBuilder extends BaseHtmlView {
  // TODO(#9218): remove this custom spacing when we update the page to match the new mocks
  private static final String SPACE_BETWEEN_FORM_ELEMENTS = "mb-4";
  // Names of form fields.
  private static final String DISPLAY_MODE_FIELD_NAME = "displayMode";
  private static final String ELIGIBILITY_FIELD_NAME = "eligibilityIsGating";
  private static final String NOTIFICATIONS_PREFERENCES_FIELD_NAME = "notificationPreferences";
  private static final String PROGRAM_TYPE_FIELD_NAME = "programType";
  private static final String TI_GROUPS_FIELD_NAME = "tiGroups[]";

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
        ProgramType.fromValue(program.getProgramType()),
        programEditStatus,
        ImmutableSet.copyOf(program.getTiGroups()),
        ImmutableList.copyOf(program.getCategories()),
        ImmutableList.copyOf(program.getApplicationSteps()));
  }

  /* Builds the form using program definition data. */
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
        program.programType(),
        programEditStatus,
        program.acls().getTiProgramViewAcls(),
        program.categories().stream()
            .map(CategoryModel::getId)
            .collect(ImmutableList.toImmutableList()),
        program.applicationSteps().stream()
            .map(
                step ->
                    Map.of(
                        /* k1= */ "title",
                        /* v1= */ step.getTitle().getDefault(),
                        /* k2= */ "description",
                        /* v2= */ step.getDescription().getDefault()))
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
      ProgramType programType,
      ProgramEditStatus programEditStatus,
      ImmutableSet<Long> selectedTi,
      ImmutableList<Long> categories,
      ImmutableList<Map<String, String>> applicationSteps) {
    boolean isCommonIntakeForm = programType.equals(ProgramType.COMMON_INTAKE_FORM);
    List<CategoryModel> categoryOptions = categoryRepository.listCategories();
    FormTag formTag = form().withMethod("POST").withId("program-details-form");

    formTag.with(
        requiredFieldsExplanationContent(),
        h2("Program setup").withClasses("py-2", "mt-6", "font-semibold"),
        // Program name
        FieldWithLabel.input()
            .setId("program-display-name-input")
            .setFieldName("localizedDisplayName")
            .setLabelText("Program name")
            .setRequired(true)
            .setValue(displayName)
            .getInputTag()
            .withClass(SPACE_BETWEEN_FORM_ELEMENTS),
        AlertComponent.renderSlimAlert(
            AlertType.INFO,
            "Short description will be visible to applicants at a future date.",
            false,
            "my-2"),
        // Short description
        FieldWithLabel.textArea()
            .setId("program-display-short-description-textarea")
            .setFieldName("localizedShortDescription")
            .setLabelText(
                "Short description of this program for the public. Maximum 100 characters.")
            .setMaxLength(100)
            .setRequired(true)
            .setValue(shortDescription)
            .getTextareaTag()
            .withClass(SPACE_BETWEEN_FORM_ELEMENTS),
        // Program url
        programUrlField(adminName, programEditStatus),
        // Admin description
        FieldWithLabel.textArea()
            .setId("program-description-textarea")
            .setFieldName("adminDescription")
            .setLabelText("Program note for administrative use only (optional)")
            .setValue(adminDescription)
            .getTextareaTag()
            .withClass(SPACE_BETWEEN_FORM_ELEMENTS),
        // Program type
        fieldset(
                div(
                        input()
                            .withId("common-intake-checkbox")
                            .withClasses("usa-checkbox__input")
                            .withType("checkbox")
                            .withName(PROGRAM_TYPE_FIELD_NAME)
                            .withValue(ProgramType.COMMON_INTAKE_FORM.getValue())
                            .withCondChecked(isCommonIntakeForm),
                        label("Set program as pre-screener")
                            .withFor("common-intake-checkbox")
                            .withClasses("usa-checkbox__label"),
                        span(ViewUtils.makeSvgToolTip(
                                "You can set one program as the ‘pre-screener’. This will pin the"
                                    + " program card to the top of the programs and services page"
                                    + " while moving other program cards below it.",
                                Icons.INFO))
                            .withClass("ml-2"))
                    .withClasses("usa-checkbox"))
            .withClasses("usa-fieldset", SPACE_BETWEEN_FORM_ELEMENTS),
        // Hidden checkbox used to signal whether or not the user has confirmed they want to
        // change which program is marked as the common intake form.
        FieldWithLabel.checkbox()
            .setId("confirmed-change-common-intake-checkbox")
            .setFieldName("confirmedChangeCommonIntakeForm")
            .setValue("false")
            .setChecked(false)
            .addStyleClass("hidden")
            .getCheckboxTag(),
        // Program Eligibility
        fieldset(
                legend("Program eligibility gating")
                    .withClass("text-gray-600")
                    .with(ViewUtils.requiredQuestionIndicator()),
                buildUSWDSRadioOption(
                    /* id= */ "program-eligibility-gating",
                    /* name= */ ELIGIBILITY_FIELD_NAME,
                    /* value= */ String.valueOf(true),
                    /* isChecked= */ eligibilityIsGating && !isCommonIntakeForm,
                    /* isDisabled= */ isCommonIntakeForm,
                    /* label= */ "Only allow residents to submit applications if they meet all"
                        + " eligibility requirements"),
                buildUSWDSRadioOption(
                    /* id= */ "program-eligibility-not-gating",
                    /* name= */ ELIGIBILITY_FIELD_NAME,
                    /* value= */ String.valueOf(false),
                    /* isChecked= */ !eligibilityIsGating,
                    /* isDisabled= */ isCommonIntakeForm,
                    /* label= */ "Allow residents to submit applications even if they don't meet"
                        + " eligibility requirements"))
            .withId("program-eligibility")
            .withClasses("usa-fieldset", SPACE_BETWEEN_FORM_ELEMENTS),
        // Program categories
        iff(
            settingsManifest.getProgramFilteringEnabled(request) && !categoryOptions.isEmpty(),
            showCategoryCheckboxes(categoryOptions, categories, isCommonIntakeForm)),
        // Program visibility
        fieldset(
                legend("Program visibility")
                    .withClass("text-gray-600")
                    .with(ViewUtils.requiredQuestionIndicator()),
                buildUSWDSRadioOption(
                    /* id= */ "program-display-mode-public",
                    /* name= */ DISPLAY_MODE_FIELD_NAME,
                    /* value= */ DisplayMode.PUBLIC.getValue(),
                    /* isChecked= */ displayMode.equals(DisplayMode.PUBLIC.getValue()),
                    /* isDisabled */ false,
                    /* label= */ "Publicly visible"),
                buildUSWDSRadioOption(
                    /* id= */ "program-display-mode-hidden",
                    /* name= */ DISPLAY_MODE_FIELD_NAME,
                    /* value= */ DisplayMode.HIDDEN_IN_INDEX.getValue(),
                    /* isChecked= */ displayMode.equals(DisplayMode.HIDDEN_IN_INDEX.getValue()),
                    /* isDisabled= */ false,
                    /* label= */ "Hide from applicants. Only individuals with the unique"
                        + " program link can access this program"),
                buildUSWDSRadioOption(
                    /* id= */ "program-display-mode-ti-only",
                    /* name= */ DISPLAY_MODE_FIELD_NAME,
                    /* value= */ DisplayMode.TI_ONLY.getValue(),
                    /* isChecked= */ displayMode.equals(DisplayMode.TI_ONLY.getValue()),
                    /* isDisabled= */ false,
                    /* label= */ "Trusted intermediaries only"),
                buildUSWDSRadioOption(
                    "program-display-mode-select-ti-only",
                    /* name= */ DISPLAY_MODE_FIELD_NAME,
                    /* value= */ DisplayMode.SELECT_TI.getValue(),
                    /* isChecked= */ displayMode.equals(DisplayMode.SELECT_TI.getValue()),
                    /* isDisabled= */ false,
                    /* label= */ " Visible to selected trusted intermediaries only"),
                showTiSelectionList(
                    selectedTi, displayMode.equals(DisplayMode.SELECT_TI.getValue())),
                buildUSWDSRadioOption(
                    /* id= */ "program-display-mode-disabled",
                    /* name= */ DISPLAY_MODE_FIELD_NAME,
                    /* value= */ DisplayMode.DISABLED.getValue(),
                    /* isChecked= */ displayMode.equals(DisplayMode.DISABLED.getValue()),
                    /* isDisabled= */ false,
                    /* label= */ "Disabled"))
            .withClasses("usa-fieldset", SPACE_BETWEEN_FORM_ELEMENTS),
        // Email notifications
        fieldset(
                legend("Email notifications").withClass("text-gray-600"),
                buildUSWDSCheckboxOption(
                    /* id= */ "notification-preferences-email",
                    /* name= */ NOTIFICATIONS_PREFERENCES_FIELD_NAME,
                    /* value= */ ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS
                        .getValue(),
                    /* isChecked= */ notificationPreferences.contains(
                        ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS
                            .getValue()),
                    /* isDisabled= */ false,
                    /* label= */ "Send Program Admins an email notification every time an"
                        + " application is submitted"))
            .withClasses("usa-fieldset", SPACE_BETWEEN_FORM_ELEMENTS),
        h2("Program overview").withClasses("py-2", "mt-6", "font-semibold"),
        // Program long description
        FieldWithLabel.textArea()
            .setId("program-display-description-textarea")
            .setFieldName("localizedDisplayDescription")
            .setLabelText("Long program description (optional)")
            .setMarkdownSupported(true)
            .setValue(displayDescription)
            .setAttribute(
                "data-northstar-enabled",
                String.valueOf(settingsManifest.getNorthStarApplicantUi(request)))
            .setDisabled(isCommonIntakeForm && settingsManifest.getNorthStarApplicantUi(request))
            .setReadOnly(isCommonIntakeForm && settingsManifest.getNorthStarApplicantUi(request))
            .getTextareaTag()
            .withClass(SPACE_BETWEEN_FORM_ELEMENTS),
        // Program external link
        FieldWithLabel.input()
            .setId("program-external-link-input")
            .setFieldName("externalLink")
            .setLabelText("Link to program website (optional)")
            .setValue(externalLink)
            .getInputTag()
            .withClass(SPACE_BETWEEN_FORM_ELEMENTS),
        h2("How to apply").withClasses("py-2", "mt-6", "font-semibold"),
        AlertComponent.renderSlimAlert(
            AlertType.INFO,
            "Application steps will be visible to applicants at a future date.",
            false,
            "my-2"),
        // Application steps
        div()
            .with(
                buildApplicationStepDiv(0, applicationSteps, isCommonIntakeForm),
                buildApplicationStepDiv(1, applicationSteps, isCommonIntakeForm),
                buildApplicationStepDiv(2, applicationSteps, isCommonIntakeForm),
                buildApplicationStepDiv(3, applicationSteps, isCommonIntakeForm),
                buildApplicationStepDiv(4, applicationSteps, isCommonIntakeForm)),
        h2("Confirmation message").withClasses("py-2", "mt-6", "font-semibold"),
        // Confirmation message
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

  static DivTag buildApplicationStepDiv(
      int i, ImmutableList<Map<String, String>> applicationSteps, boolean isCommonIntakeForm) {

    // Fill in the existing application steps
    String titleValue = "";
    String descriptionValue = "";
    if (i + 1 <= applicationSteps.size()) {
      titleValue = applicationSteps.get(i).get("title");
      descriptionValue = applicationSteps.get(i).get("description");
    }

    String index = Integer.toString(i);
    String indexPlusOne = Integer.toString(i + 1);

    FieldWithLabel title =
        FieldWithLabel.input()
            .setId("apply-step-" + indexPlusOne + "-title")
            .setFieldName("applicationSteps[" + index + "][title]")
            .setDisabled(isCommonIntakeForm)
            .setReadOnly(isCommonIntakeForm)
            .setValue(titleValue);

    FieldWithLabel description =
        FieldWithLabel.textArea()
            .setId("apply-step-" + indexPlusOne + "-description")
            .setFieldName("applicationSteps[" + index + "][description]")
            .setDisabled(isCommonIntakeForm)
            .setReadOnly(isCommonIntakeForm)
            .setMarkdownSupported(true)
            .setValue(descriptionValue);

    Boolean isRequired = indexPlusOne.equals("1") && !isCommonIntakeForm;
    if (isRequired) {
      title.setLabelText("Step 1 title").setRequired(true);
      description.setLabelText("Step 1 description").setRequired(true);
    } else {
      title.setLabelText("Step " + indexPlusOne + " title (optional)");
      description.setLabelText("Step " + indexPlusOne + " description (optional)");
    }

    return div()
        .withId("apply-step-" + indexPlusOne + "-div")
        .with(title.getInputTag(), description.getTextareaTag());
  }

  private FieldsetTag showCategoryCheckboxes(
      List<CategoryModel> categoryOptions, List<Long> categories, boolean isCommonIntakeForm) {
    return fieldset(
            legend(
                    "Tag this program with 1 or more categories to make it easier to find"
                        + " (optional)")
                .withClass("text-gray-600"),
            div(each(
                    categoryOptions,
                    category ->
                        buildUSWDSCheckboxOption(
                                /* id= */ "checkbox-category-" + category.getDefaultName(),
                                /* name= */ "categories" + Path.ARRAY_SUFFIX,
                                /* value= */ String.valueOf(category.getId()),
                                /* isChecked= */ categories.contains(category.getId())
                                    && !isCommonIntakeForm,
                                /* isDisabled= */ isCommonIntakeForm,
                                /* label= */ category.getDefaultName())
                            .withClasses("grid-col-12", "tablet:grid-col-6")))
                .withClasses("grid-row", "grid-gap-md"))
        .withId("category-checkboxes")
        .withClasses("usa-fieldset", SPACE_BETWEEN_FORM_ELEMENTS);
  }

  private DomContent showTiSelectionList(ImmutableSet<Long> selectedTi, boolean selectTiChecked) {
    List<TrustedIntermediaryGroupModel> tiGroups =
        accountRepository.listTrustedIntermediaryGroups();

    DivTag tiDiv =
        div(
                // Hidden input that's always selected to allow for clearing multi-select data.
                buildUSWDSCheckboxOption(
                        /* id= */ "",
                        /* name= */ TI_GROUPS_FIELD_NAME,
                        /* value= */ "",
                        /* isChecked= */ true,
                        /* isDisabled= */ false,
                        /* label= */ "")
                    .withClasses("hidden"),
                each(
                    tiGroups,
                    tiGroup ->
                        buildUSWDSCheckboxOption(
                            /* id= */ tiGroup.id.toString(),
                            /* name= */ TI_GROUPS_FIELD_NAME,
                            /* value= */ tiGroup.id.toString(),
                            /* isChecked= */ selectedTi.contains(tiGroup.id),
                            /* isDisabled= */ false,
                            /* label= */ tiGroup.getName())))
            .withId("TiList")
            .withClasses("px-4", "py-2");
    return selectTiChecked ? tiDiv : tiDiv.isHidden();
  }

  private DomContent programUrlField(String adminName, ProgramEditStatus programEditStatus) {
    if (programEditStatus != ProgramEditStatus.CREATION) {
      // Only allow editing the program URL at program creation time.
      String programUrl =
          baseUrl
              + routes.ApplicantProgramsController.show(MainModule.SLUGIFIER.slugify(adminName))
                  .url();
      return div()
          .withClass(SPACE_BETWEEN_FORM_ELEMENTS)
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
        .getInputTag()
        .withClass(SPACE_BETWEEN_FORM_ELEMENTS);
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

  private DivTag buildUSWDSRadioOption(
      String id, String name, String value, Boolean isChecked, Boolean isDisabled, String label) {
    return div(
            input()
                .withId(id)
                .withClasses("usa-radio__input usa-radio__input--tile")
                .withType("radio")
                .withName(name)
                .withValue(value)
                .withCondChecked(isChecked)
                .withCondDisabled(isDisabled),
            label(label).withFor(id).withClasses("usa-radio__label"))
        .withClasses("usa-radio");
  }

  private DivTag buildUSWDSCheckboxOption(
      String id, String name, String value, Boolean isChecked, Boolean isDisabled, String label) {
    return div(
            input()
                .withId(id)
                .withClasses("usa-checkbox__input usa-checkbox__input--tile")
                .withType("checkbox")
                .withName(name)
                .withValue(value)
                .withCondChecked(isChecked)
                .withCondDisabled(isDisabled),
            label(label).withFor(id).withClasses("usa-checkbox__label"))
        .withClasses("usa-checkbox");
  }
}
