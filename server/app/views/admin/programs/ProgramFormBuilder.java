package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static featureflags.FeatureFlag.INTAKE_FORM_ENABLED;
import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import com.typesafe.config.Config;
import featureflags.FeatureFlags;
import forms.ProgramForm;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import models.DisplayMode;
import modules.MainModule;
import play.mvc.Http.Request;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import views.BaseHtmlView;
import views.ViewUtils;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.Modal;
import views.components.Modal.Width;
import views.style.AdminStyles;
import views.style.BaseStyles;

/**
 * Builds a program form for rendering. If the program was previously created, the {@code adminName}
 * field is disabled, since it cannot be edited once set.
 */
abstract class ProgramFormBuilder extends BaseHtmlView {

  private final FeatureFlags featureFlags;
  private final String baseUrl;

  ProgramFormBuilder(Config configuration, FeatureFlags featureFlags) {
    this.featureFlags = featureFlags;
    this.baseUrl = checkNotNull(configuration).getString("base_url");
  }

  /** Builds the form using program form data. */
  protected final FormTag buildProgramForm(
      Request request, ProgramForm program, boolean editExistingProgram) {
    return buildProgramForm(
        request,
        program.getAdminName(),
        program.getAdminDescription(),
        program.getLocalizedDisplayName(),
        program.getLocalizedDisplayDescription(),
        program.getExternalLink(),
        program.getLocalizedConfirmationMessage(),
        program.getDisplayMode(),
        program.getIsCommonIntakeForm(),
        editExistingProgram);
  }

  /** Builds the form using program definition data. */
  protected final FormTag buildProgramForm(
      Request request, ProgramDefinition program, boolean editExistingProgram) {
    return buildProgramForm(
        request,
        program.adminName(),
        program.adminDescription(),
        program.localizedName().getDefault(),
        program.localizedDescription().getDefault(),
        program.externalLink(),
        program.localizedConfirmationMessage().getDefault(),
        program.displayMode().getValue(),
        program.programType().equals(ProgramType.COMMON_INTAKE_FORM),
        editExistingProgram);
  }

  private FormTag buildProgramForm(
      Request request,
      String adminName,
      String adminDescription,
      String displayName,
      String displayDescription,
      String externalLink,
      String confirmationSceen,
      String displayMode,
      Boolean isCommonIntakeForm,
      boolean editExistingProgram) {
    FormTag formTag = form().withMethod("POST").withId("program-details-form");
    formTag.with(
        requiredFieldsExplanationContent(),
        h2("Visible to applicants").withClasses("py-2"),
        FieldWithLabel.input()
            .setId("program-display-name-input")
            .setFieldName("localizedDisplayName")
            .setLabelText("Enter the publicly displayed name for this program*")
            .setValue(displayName)
            .getInputTag(),
        FieldWithLabel.textArea()
            .setId("program-display-description-textarea")
            .setFieldName("localizedDisplayDescription")
            .setLabelText("Describe this program for the public*")
            .setValue(displayDescription)
            .getTextareaTag(),
        programUrlField(adminName, editExistingProgram),
        FieldWithLabel.input()
            .setId("program-external-link-input")
            .setFieldName("externalLink")
            .setLabelText("Link to program website")
            .setValue(externalLink)
            .getInputTag(),
        FieldWithLabel.textArea()
            .setId("program-confirmation-message-textarea")
            .setFieldName("localizedConfirmationMessage")
            .setLabelText(
                "A custom message that will be shown on the confirmation page after an application"
                    + " has been submitted. You can use this message to explain next steps of the"
                    + " application process and/or highlight other programs to apply for.")
            .setValue(confirmationSceen)
            .getTextareaTag(),
        h2("Visible to administrators only").withClasses("py-2"),
        // TODO(#2618): Consider using helpers for grouping related radio controls.
        fieldset()
            .with(
                legend("Program visibility*").withClass(BaseStyles.INPUT_LABEL),
                FieldWithLabel.radio()
                    .setId("program-display-mode-public")
                    .setFieldName("displayMode")
                    .setLabelText("Publicly visible")
                    .setValue(DisplayMode.PUBLIC.getValue())
                    .setChecked(displayMode.equals(DisplayMode.PUBLIC.getValue()))
                    .getRadioTag(),
                FieldWithLabel.radio()
                    .setId("program-display-mode-hidden")
                    .setFieldName("displayMode")
                    .setLabelText(
                        "Hide from applicants. Only individuals with the unique program link can"
                            + " access this program")
                    .setValue(DisplayMode.HIDDEN_IN_INDEX.getValue())
                    .setChecked(displayMode.equals(DisplayMode.HIDDEN_IN_INDEX.getValue()))
                    .getRadioTag()),
        FieldWithLabel.textArea()
            .setId("program-description-textarea")
            .setFieldName("adminDescription")
            .setLabelText("Program note for administrative use only*")
            .setValue(adminDescription)
            .getTextareaTag());
    if (featureFlags.getFlagEnabled(request, INTAKE_FORM_ENABLED)) {
      formTag
          .with(
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
                          .withClass("ml-2")))
          .with(
              // Hidden checkbox used to signal whether or not the user has confirmed they want to
              // change which program is marked as the common intake form.
              FieldWithLabel.checkbox()
                  .setId("confirmed-change-common-intake-checkbox")
                  .setFieldName("confirmedChangeCommonIntakeForm")
                  .setValue("false")
                  .setChecked(false)
                  .addStyleClass("hidden")
                  .getCheckboxTag());
    }
    formTag.with(
        submitButton("Save")
            .withId("program-update-button")
            .withClasses(AdminStyles.PRIMARY_BUTTON_STYLES));

    return formTag;
  }

  private DomContent programUrlField(String adminName, boolean editExistingProgram) {
    if (editExistingProgram) {
      String programUrl =
          baseUrl
              + controllers.applicant.routes.RedirectController.programBySlug(
                      MainModule.SLUGIFIER.slugify(adminName))
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
                + " a dash between each word*")
        .setValue(adminName)
        .getInputTag();
  }

  protected Modal buildConfirmCommonIntakeChangeModal(String existingCommonIntakeFormDisplayName) {
    DivTag content =
        div()
            .withClasses("pl-6", "pr-6", "flex-row", "space-y-6")
            .with(
                p("The pre-screener will be updated from ")
                    .with(span(existingCommonIntakeFormDisplayName).withClass("font-bold"))
                    .withText(" to the current program."))
            .with(p("Would you like to confirm the change?"))
            .with(
                submitButton("Confirm")
                    .withForm("program-details-form")
                    .withId("confirm-common-intake-change-button")
                    .withClasses(AdminStyles.PRIMARY_BUTTON_STYLES, "cursor-pointer"));
    return Modal.builder("confirm-common-intake-change", content)
        .setModalTitle("Confirm pre-screener change?")
        .setDisplayOnLoad(true)
        .setWidth(Width.THIRD)
        .build();
  }
}
