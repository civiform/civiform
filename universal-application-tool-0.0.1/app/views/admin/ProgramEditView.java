package views.admin;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.join;

import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;

public class ProgramEditView extends BaseHtmlView {
  public Content render(Request request, ProgramDefinition program) {
    return htmlContent(
        body(
            div(join("Edit program:", program.name())),
            div(
                form(
                        makeCsrfTokenInputTag(request),
                        div(textFieldWithValue("name", "Program Name", program.name())),
                        div(
                            textFieldWithValue(
                                "description", "Program Description", program.description())),
                        submitButton("Save"))
                    .withMethod("post")
                    .withAction(
                        controllers.admin.routes.AdminProgramController.update(program.id())
                            .url()))));
  }
}
