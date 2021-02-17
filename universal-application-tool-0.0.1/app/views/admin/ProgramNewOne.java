package views.admin;

import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.ViewUtils;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.*;

public final class ProgramNewOne extends BaseHtmlView {

  private final ViewUtils viewUtils;

  @Inject
  public ProgramNewOne(ViewUtils viewUtils) {
    this.viewUtils = checkNotNull(viewUtils);
  }

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
                    .withAction("/admin/programs"))));
  }
}
