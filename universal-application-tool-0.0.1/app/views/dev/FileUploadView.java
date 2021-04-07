package views.dev;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;
import static j2html.TagCreator.input;
import static j2html.TagCreator.title;
import static j2html.attributes.Attr.ENCTYPE;

import controllers.dev.routes;
import javax.inject.Inject;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.BaseHtmlView;

public class FileUploadView extends BaseHtmlView {
  private final BaseHtmlLayout layout;

  @Inject
  public FileUploadView(BaseHtmlLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(Request request) {
    return layout.htmlContent(
        head(title("Dev File Upload"), layout.tailwindStyles()),
        body()
            .with(h1("Dev File Upload"))
            .with(
                div()
                    .with(
                        form()
                            .with(makeCsrfTokenInputTag(request))
                            .attr(ENCTYPE, "multipart/form-data")
                            .with(input().withType("file").withId("myFile").withName("filename"))
                            .with(submitButton("Upload file"))
                            .withMethod("post")
                            .withAction(routes.FileUploadController.upload().url()))));
  }
}
