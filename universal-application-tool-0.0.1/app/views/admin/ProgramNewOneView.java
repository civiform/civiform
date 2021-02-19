package views.admin;

import static j2html.TagCreator.*;

import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.BaseHtmlView;

public final class ProgramNewOneView extends BaseHtmlView {

  public Content render(Request request) {
    return htmlContent(
        body(
            h1("Create a new Program"),
            div(
                form(
                        makeCsrfTokenInputTag(request),
                        textField("name", "Program Name"),
                        textField("description", "Program Description"),
                        submitButton("Create"))
                    .withMethod("post")
                    .withAction(controllers.admin.routes.AdminProgramController.index().url()))));
  }
}
