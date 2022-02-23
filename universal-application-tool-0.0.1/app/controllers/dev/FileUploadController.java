package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.util.Comparator;
import java.util.Set;
import models.StoredFile;
import play.Environment;
import play.i18n.MessagesApi;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;
import services.cloud.FileNameFormatter;
import services.cloud.StorageClient;
import services.cloud.StorageUploadRequest;
import views.dev.FileUploadView;

/**
 * Controller for interacting with cloud storage provider emulators directly. The logic for
 * uploading files to cloud storage has been extracted out into {@link
 * CloudEmulatorFileStorageStrategy}
 */
public class FileUploadController extends DevController {

  private final FileUploadView view;
  private final StorageClient storageClient;
  private final StoredFileRepository storedFileRepository;
  private final String baseUrl;
  private final CloudEmulatorFileStorageStrategy cloudEmulatorFileStorageStrategy;
  private final MessagesApi messagesApi;

  @Inject
  public FileUploadController(
      FileUploadView view,
      StorageClient storageClient,
      StoredFileRepository storedFileRepository,
      Environment environment,
      Config configuration,
      CloudEmulatorFileStorageStrategy cloudEmulatorFileStorageStrategy,
      MessagesApi messagesApi) {
    super(environment, configuration);
    this.view = checkNotNull(view);
    this.storageClient = checkNotNull(storageClient);
    this.storedFileRepository = checkNotNull(storedFileRepository);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
    this.cloudEmulatorFileStorageStrategy = checkNotNull(cloudEmulatorFileStorageStrategy);
    this.messagesApi = checkNotNull(messagesApi);
  }

  public Result index(Request request) {
    if (!isDevEnvironment()) {
      return notFound();
    }

    StorageUploadRequest signedRequest =
        storageClient.getSignedUploadRequest(
            FileNameFormatter.formatDevUploadFilename(),
            baseUrl + routes.FileUploadController.create().url());
    Set<StoredFile> files = storedFileRepository.list().toCompletableFuture().join();
    ImmutableList<StoredFile> fileList =
        files.stream()
            .sorted(Comparator.comparing(StoredFile::getName))
            .collect(ImmutableList.toImmutableList());
    return ok(
        view.render(
            request,
            signedRequest,
            fileList,
            request.flash().get("success"),
            messagesApi.preferred(request)));
  }

  public Result create(Request request) {
    if (!isDevEnvironment()) {
      return notFound();
    }
    return cloudEmulatorFileStorageStrategy.create(storedFileRepository, request);
  }
}
