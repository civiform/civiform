package views.admin.programs;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;

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
            renderHeader("New program"),
            div(
                form(
                        makeCsrfTokenInputTag(request),
                        FieldWithLabel.input()
                            .setId("program-name-input")
                            .setFieldName("name")
                            .setLabelText("Program name")
                            .setPlaceholderText("The name of the program")
                            .getContainer(),
                        FieldWithLabel.textArea()
                            .setId("program-description-textarea")
                            .setFieldName("description")
                            .setLabelText("Program description")
                            .setPlaceholderText("The description of the program")
                            .getContainer(),
                        submitButton("Create").withId("program-create-button"))
                    .withMethod("post")
                    .withAction(controllers.admin.routes.AdminProgramController.index().url()))));
  }
}
