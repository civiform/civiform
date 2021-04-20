package views.admin.programs;

import static j2html.TagCreator.form;

import j2html.tags.ContainerTag;
import views.BaseHtmlView;
import views.components.FieldWithLabel;

/**
 * Builds a program form for rendering. If the program was previously created, the {@code adminName}
 * field is disabled, since it cannot be edited once set.
 */
public class ProgramFormBuilder extends BaseHtmlView {

  public static ContainerTag buildProgramForm(
      String adminName,
      String adminDescription,
      String displayName,
      String displayDescription,
      boolean programExists) {
    ContainerTag formTag = form().withMethod("POST");
    formTag.with(
        FieldWithLabel.input()
            .setId("program-name-input")
            .setFieldName("adminName")
            .setLabelText("What do you want to call this program?")
            .setPlaceholderText(
                "Give a name for internal identification purposes - this cannot be updated once"
                    + " set")
            .setValue(adminName)
            .setDisabled(programExists)
            .getContainer(),
        FieldWithLabel.textArea()
            .setId("program-description-textarea")
            .setFieldName("adminDescription")
            .setLabelText("Program description")
            .setPlaceholderText("This description is visible only to system admins")
            .setValue(adminDescription)
            .getContainer(),
        FieldWithLabel.input()
            .setId("program-display-name-textarea")
            .setFieldName("localizedDisplayName")
            .setLabelText("Program display name")
            .setPlaceholderText(
                "What is the name of this program? This will be shown to applicants")
            .setValue(displayName)
            .getContainer(),
        FieldWithLabel.textArea()
            .setId("program-description-textarea")
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
