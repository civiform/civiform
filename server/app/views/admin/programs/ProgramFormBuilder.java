package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.input;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.p;

import com.typesafe.config.Config;
import forms.ProgramForm;
import j2html.tags.DomContent;
import j2html.tags.specialized.FormTag;
import models.DisplayMode;
import modules.MainModule;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.components.FieldWithLabel;
import views.style.BaseStyles;
import views.style.Styles;

/**
 * Builds a program form for rendering. If the program was previously created, the {@code adminName}
 * field is disabled, since it cannot be edited once set.
 */
abstract class ProgramFormBuilder extends BaseHtmlView {

  private final String baseUrl;

  ProgramFormBuilder(Config configuration) {
    this.baseUrl = checkNotNull(configuration).getString("base_url");
  }

  /** Builds the form using program form data. */
  protected final FormTag buildProgramForm(ProgramForm program, boolean editExistingProgram) {
    return buildProgramForm(
        program.getAdminName(),
        program.getAdminDescription(),
        program.getLocalizedDisplayName(),
        program.getLocalizedDisplayDescription(),
        program.getExternalLink(),
        program.getDisplayMode(),
        editExistingProgram);
  }

  /** Builds the form using program definition data. */
  protected final FormTag buildProgramForm(ProgramDefinition program, boolean editExistingProgram) {
    return buildProgramForm(
        program.adminName(),
        program.adminDescription(),
        program.localizedName().getDefault(),
        program.localizedDescription().getDefault(),
        program.externalLink(),
        program.displayMode().getValue(),
        editExistingProgram);
  }

  private FormTag buildProgramForm(
      String adminName,
      String adminDescription,
      String displayName,
      String displayDescription,
      String externalLink,
      String displayMode,
      boolean editExistingProgram) {
    FormTag formTag = form().withMethod("POST");
    formTag.with(
        requiredFieldsExplanationContent(),
        h2("Visible to applicants").withClasses(Styles.PY_2),
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
        h2("Visible to administrators only").withClasses(Styles.PY_2),
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
            .getTextareaTag(),
        submitButton("Save").withId("program-update-button"));
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
          .withClass(Styles.MB_2)
          .with(
              input().isHidden().withName("adminName").withValue(adminName),
              p("The URL for this program. This value can't be changed")
                  .withClasses(BaseStyles.INPUT_LABEL),
              a(programUrl).withClasses(BaseStyles.FORM_FIELD));
    }
    return FieldWithLabel.input()
        .setId("program-name-input")
        .setFieldName("adminName")
        .setLabelText(
            "Enter the URL for this program. This value can't be changed later. Aim to keep it"
                + " short so it's easy to share. Use a dash between each word*")
        .setValue(adminName)
        .getInputTag();
  }
}
