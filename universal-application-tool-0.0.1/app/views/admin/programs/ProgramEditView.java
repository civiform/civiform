package views.admin.programs;

import static j2html.TagCreator.div;
import static j2html.TagCreator.form;

import com.google.inject.Inject;
import forms.ProgramForm;
import j2html.tags.ContainerTag;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.LinkElement;
import views.style.Styles;

public class ProgramEditView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramEditView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(Request request, ProgramDefinition program) {
    ContainerTag formTag =
        buildProgramForm(program.adminName(), program.adminDescription())
            .with(makeCsrfTokenInputTag(request))
            .with(buildManageQuestionLink(program.id()))
            .withAction(controllers.admin.routes.AdminProgramController.update(program.id()).url());
    return layout.render(
        renderHeader(String.format("Edit program: %s", program.adminName())), formTag);
  }

  public Content render(Request request, long id, ProgramForm program, String message) {
    ContainerTag formTag =
        buildProgramForm(program.getName(), program.getDescription())
            .with(makeCsrfTokenInputTag(request))
            .with(buildManageQuestionLink(id))
            .withAction(controllers.admin.routes.AdminProgramController.update(id).url());
    return layout.render(
        renderHeader(String.format("Edit program: %s", program.getName())), div(message), formTag);
  }

  private ContainerTag buildProgramForm(String programName, String programDescription) {
    ContainerTag formTag = form().withMethod("POST");
    formTag.with(
        FieldWithLabel.input()
            .setId("program-name-input")
            .setFieldName("name")
            .setLabelText("What do you want to call this program?")
            .setPlaceholderText(
                "Give a name for internal identification purposes - this cannot be updated once"
                    + " set")
            .setValue(programName)
            .getContainer(),
        FieldWithLabel.textArea()
            .setId("program-description-textarea")
            .setFieldName("description")
            .setLabelText("Program description")
            .setPlaceholderText("This description is visible only to system admins")
            .setValue(programDescription)
            .getContainer(),
        submitButton("Save").withId("program-update-button"));
    return formTag;
  }

  private ContainerTag buildManageQuestionLink(long id) {
    String manageQuestionLink =
        controllers.admin.routes.AdminProgramBlocksController.index(id).url();
    return new LinkElement()
        .setId("manage-questions-link")
        .setHref(manageQuestionLink)
        .setText("Manage Questions →")
        .setStyles(Styles.MX_4, Styles.FLOAT_RIGHT)
        .asAnchorText();
  }
}
