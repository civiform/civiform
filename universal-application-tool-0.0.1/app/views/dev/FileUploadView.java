package views.dev;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.head;
import static j2html.TagCreator.input;
import static j2html.TagCreator.pre;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.title;
import static j2html.TagCreator.tr;
import static j2html.attributes.Attr.ENCTYPE;

import com.google.common.collect.ImmutableList;
import controllers.dev.routes;
import j2html.tags.Tag;
import javax.inject.Inject;
import models.StoredFile;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.style.Styles;

public class FileUploadView extends BaseHtmlView {
  private final BaseHtmlLayout layout;

  @Inject
  public FileUploadView(BaseHtmlLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(Request request, ImmutableList<StoredFile> files) {
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
                            .withAction(routes.FileUploadController.upload().url())))
            .with(
                div()
                    .withClasses(Styles.GRID, Styles.GRID_COLS_2)
                    .with(div().with(h2("Current Files:")).with(pre(renderFiles(files))))));
  }

  private Tag renderFiles(ImmutableList<StoredFile> files) {
    return table()
        .with(
            tbody(
                each(
                    files,
                    file ->
                        tr(
                            td(String.valueOf(file.id)),
                            td(a(file.getName()).withHref(file.getPresignedURL().toString()))))));
  }
}
