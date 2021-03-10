package views.admin.programs;

import static j2html.TagCreator.form;

import com.google.inject.Inject;
import j2html.tags.ContainerTag;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.Styles;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;

public class ProgramEditView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramEditView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(Request request, ProgramDefinition program) {
    String manageQuestionLink =
        controllers.admin.routes.AdminProgramBlocksController.index(program.id()).url();
    ContainerTag formTag =
        form(
                makeCsrfTokenInputTag(request),
                FieldWithLabel.createInput("name")
                    .setLabelText("Program name")
                    .setPlaceholderText("The name of the program")
                    .setValue(program.name())
                    .getContainer(),
                FieldWithLabel.createTextArea("description")
                    .setLabelText("Program description")
                    .setPlaceholderText("The description of the program")
                    .setValue(program.description())
                    .getContainer(),
                submitButton("Save"),
                renderLink(
                        "Manage Questions →",
                        manageQuestionLink,
                        String.join(" ", Styles.MX_4, Styles.FLOAT_RIGHT))
                    .withId("manageQuestions"))
            .withMethod("post")
            .withAction(controllers.admin.routes.AdminProgramController.update(program.id()).url());
    return layout.render(renderHeader(String.format("Edit program: %s", program.name())), formTag);
  }
}
