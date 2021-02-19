package views.admin;


import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.BaseHtmlView;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;

public final class ProgramNewOneView extends BaseHtmlView {

  public Content render(Request request) {
    return htmlContent(
        body(
            h1("Create a new Program"),
            div(
                form(
                        makeCsrfTokenInputTag(request),
                        div(textField("name", "Program Name")),
                        div(textField("description", "Program Description")),
                        submitButton("Create"))
                    .withMethod("post")
                    .withAction(controllers.admin.routes.AdminProgramController.index().url()))));
  }
}
