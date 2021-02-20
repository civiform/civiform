package views.admin;

import static j2html.TagCreator.*;

import com.google.inject.Inject;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.BaseHtmlView;

public final class ProgramNewOneView extends BaseHtmlView {
  private final BaseHtmlLayout layout;

  @Inject
  public ProgramNewOneView(BaseHtmlLayout layout) {
    this.layout = layout;
  }

  public Content render(Request request) {
    return layout.htmlContent(
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
