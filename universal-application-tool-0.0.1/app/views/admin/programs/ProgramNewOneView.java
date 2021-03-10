package views.admin.programs;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;

import com.google.inject.Inject;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;

public final class ProgramNewOneView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramNewOneView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(Request request) {
    return layout.render(
        body(
            h1("Create a new Program"),
            div(
                form(
                        makeCsrfTokenInputTag(request),
                        FieldWithLabel.createInput("name")
                            .setLabelText("Program name")
                            .setPlaceholderText("The name of the program")
                            .getContainer(),
                        FieldWithLabel.createTextArea("description")
                            .setLabelText("Program description")
                            .setPlaceholderText("The description of the program")
                            .getContainer(),
                        submitButton("Create"))
                    .withMethod("post")
                    .withAction(controllers.admin.routes.AdminProgramController.index().url()))));
  }
}
