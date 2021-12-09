package views.dev;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.input;
import static j2html.TagCreator.pre;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.tr;
import static j2html.attributes.Attr.ENCTYPE;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import java.util.Optional;
import javax.inject.Inject;
import models.StoredFile;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.cloud.aws.SignedS3UploadRequest;
import services.cloud.aws.SimpleStorage;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.style.Styles;

/** Renders a page for a developer to test uploading files. */
public class FileUploadView extends BaseHtmlView {
  private final BaseHtmlLayout layout;
  private final SimpleStorage s3Client;

  @Inject
  public FileUploadView(BaseHtmlLayout layout, SimpleStorage s3Client) {
    this.layout = checkNotNull(layout);
    this.s3Client = checkNotNull(s3Client);
  }

  public Content render(
      Request request,
      SignedS3UploadRequest signedRequest,
      ImmutableList<StoredFile> files,
      Optional<String> maybeFlash) {
    String title = "Dev file upload";
    HtmlBundle bundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(
                div()
                    .with(div(maybeFlash.orElse("")))
                    .with(h1(title))
                    .with(div().with(fileUploadForm(signedRequest)))
                    .with(
                        div()
                            .withClasses(Styles.GRID, Styles.GRID_COLS_2)
                            .with(div().with(h2("Current Files:")).with(pre(renderFiles(files))))));
    return layout.render(bundle);
  }

  private ContainerTag renderFiles(ImmutableList<StoredFile> files) {
    return table()
        .with(
            tbody(
                each(
                    files,
                    file ->
                        tr(
                            td(String.valueOf(file.id)),
                            td(a(file.getName()).withHref(getPresignedURL(file)))))));
  }

  private String getPresignedURL(StoredFile file) {
    return s3Client.getPresignedUrl(file.getName()).toString();
  }

  private ContainerTag fileUploadForm(SignedS3UploadRequest request) {
    ContainerTag formTag =
        form()
            .attr(ENCTYPE, "multipart/form-data")
            .with(input().withType("input").withName("key").withValue(request.key()))
            .with(
                input()
                    .withType("hidden")
                    .withName("success_action_redirect")
                    .withValue(request.successActionRedirect()))
            .with(
                input()
                    .withType("text")
                    .withName("X-Amz-Credential")
                    .withValue(request.credential()));
    if (!request.securityToken().isEmpty()) {
      formTag.with(
          input()
              .withType("hidden")
              .withName("X-Amz-Security-Token")
              .withValue(request.securityToken()));
    }
    return formTag
        .with(input().withType("text").withName("X-Amz-Algorithm").withValue(request.algorithm()))
        .with(input().withType("text").withName("X-Amz-Date").withValue(request.date()))
        .with(input().withType("hidden").withName("Policy").withValue(request.policy()))
        .with(input().withType("hidden").withName("X-Amz-Signature").withValue(request.signature()))
        .with(input().withType("file").withName("file"))
        .with(submitButton("Upload to Amazon S3"))
        .withMethod("post")
        .withAction(request.actionLink());
  }
}
