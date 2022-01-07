package views.dev;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.pre;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import java.util.Optional;
import javax.inject.Inject;
import models.StoredFile;
import play.mvc.Http.Request;
import play.twirl.api.Content;
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
      Optional<String> maybeFlash) {
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
                                .with(h2("Current Files:"))
                                .with(pre(storageStrategy.renderFiles(files, storageClient))))));
    return layout.render(bundle);
  }
}
