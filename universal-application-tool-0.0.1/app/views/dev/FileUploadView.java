package views.dev;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.pre;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.tr;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import java.util.Optional;
import javax.inject.Inject;
import models.StoredFile;
import play.i18n.Messages;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.MessageKey;
import services.cloud.StorageClient;
import services.cloud.StorageUploadRequest;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.ViewUtils;
import views.style.Styles;

/**
 * Renders a page for a developer to test uploading files. The logic for creating the file upload
 * form has been extracted out to {@link CloudStorageDevViewStrategy}.
 */
public class FileUploadView extends BaseHtmlView {

  private final BaseHtmlLayout layout;
  private final StorageClient storageClient;
  private final ViewUtils viewUtils;
  private final CloudStorageDevViewStrategy storageStrategy;

  @Inject
  public FileUploadView(
      BaseHtmlLayout layout,
      StorageClient storageClient,
      ViewUtils viewUtils,
      CloudStorageDevViewStrategy storageStrategy) {
    this.layout = checkNotNull(layout);
    this.storageClient = checkNotNull(storageClient);
    this.viewUtils = checkNotNull(viewUtils);
    this.storageStrategy = checkNotNull(storageStrategy);
  }

  public Content render(
      Request request,
      StorageUploadRequest signedRequest,
      ImmutableList<StoredFile> files,
      Optional<String> maybeFlash,
      Messages messages) {
    String title = "Dev file upload";
    ContainerTag fileUploadForm;

    HtmlBundle bundle = layout.getBundle();
    try {
      fileUploadForm = storageStrategy.getFileUploadForm(viewUtils, signedRequest, bundle);
    } catch (RuntimeException e) {
      // Exception is only thrown if there is a mismatch between the signedRequest and the cloud
      // provider.
      // For example, passing a BlobStorageUploadRequest into AwsStorageDevViewStrategy. This should
      // never happen.
      return null;
    }
    bundle
        .setTitle(title)
        .addMainContent(
            div()
                .with(div(maybeFlash.orElse("")))
                .with(h1(title))
                .with(div().with(fileUploadForm))
                .with(
                    div()
                        .withClasses(Styles.GRID, Styles.GRID_COLS_2)
                        .with(
                            div()
                                .with(
                                    h2(
                                        messages.at(
                                            MessageKey.FILEUPLOAD_LABEL_CURRENT_FILES
                                                .getKeyName())))
                                .with(pre(renderFiles(files))))));
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
    return storageClient.getPresignedUrl(file.getName()).toString();
  }
}
