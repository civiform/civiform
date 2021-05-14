package views.admin.programs;

import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;

import forms.ProgramForm;
import j2html.tags.ContainerTag;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.components.FieldWithLabel;

/**
 * Builds a program form for rendering. If the program was previously created, the {@code adminName}
 * field is disabled, since it cannot be edited once set.
 */
public class ProgramFormBuilder extends BaseHtmlView {

  /** Builds the form using program form data. */
  public static ContainerTag buildProgramForm(ProgramForm program, boolean editExistingProgram) {
    return buildProgramForm(
        program.getAdminName(),
        program.getAdminDescription(),
        program.getLocalizedDisplayName(),
        program.getLocalizedDisplayDescription(),
        editExistingProgram);
  }

  /** Builds the form using program definition data. */
  public static ContainerTag buildProgramForm(
      ProgramDefinition program, boolean editExistingProgram) {
    return buildProgramForm(
        program.adminName(),
        program.adminDescription(),
        program.localizedName().getDefault(),
        program.localizedDescription().getDefault(),
        editExistingProgram);
  }

  private static ContainerTag buildProgramForm(
      String adminName,
      String adminDescription,
      String displayName,
      String displayDescription,
      boolean editExistingProgram) {
    ContainerTag formTag = form().withMethod("POST");
    formTag.with(
        h2("Program Information - Administrative Use Only"),
        FieldWithLabel.input()
            .setId("program-name-input")
            .setFieldName("adminName")
            .setLabelText("What do you want to call this program?")
            .setPlaceholderText(
                "Give a name for internal identification purposes - this cannot be updated once"
                    + " set")
            .setValue(adminName)
            .setDisabled(editExistingProgram)
            .getContainer(),
        FieldWithLabel.textArea()
            .setId("program-description-textarea")
            .setFieldName("adminDescription")
            .setLabelText("Program description")
            .setPlaceholderText("This description is visible only to system admins")
            .setValue(adminDescription)
            .getContainer(),
        h2("Publicly Visible Program Information"),
        FieldWithLabel.input()
            .setId("program-display-name-input")
            .setFieldName("localizedDisplayName")
            .setLabelText("Program display name")
            .setPlaceholderText(
                "What is the name of this program? This will be shown to applicants")
            .setValue(displayName)
            .getContainer(),
        FieldWithLabel.textArea()
            .setId("program-display-description-textarea")
            .setFieldName("localizedDisplayDescription")
            .setLabelText("Program display description")
            .setPlaceholderText(
                "A short description of this program. This will be shown to applicants")
            .setValue(displayDescription)
            .getContainer(),
        submitButton("Save").withId("program-update-button"));
    return formTag;
  }
}
