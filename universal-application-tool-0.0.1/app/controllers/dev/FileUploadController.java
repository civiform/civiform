package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.util.Comparator;
import java.util.Set;
import models.StoredFile;
import play.Environment;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;
import services.cloud.StorageClient;
import services.cloud.StorageUploadRequest;
import views.dev.FileUploadView;

/** Controller for interacting with S3 directly in dev mode. */
public class FileUploadController extends DevController {

  private final FileUploadView view;
  private final StorageClient storageClient;
  private final StoredFileRepository storedFileRepository;
  private final String baseUrl;
  private final CloudStorageStrategy cloudStorageStrategy;

  @Inject
  public FileUploadController(
      FileUploadView view,
      StorageClient storageClient,
      StoredFileRepository storedFileRepository,
      Environment environment,
      Config configuration,
      CloudStorageStrategy cloudStorageStrategy) {
    super(environment, configuration);
    this.view = checkNotNull(view);
    this.storageClient = checkNotNull(storageClient);
    this.storedFileRepository = checkNotNull(storedFileRepository);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
    this.cloudStorageStrategy = checkNotNull(cloudStorageStrategy);
  }

  public Result index(Request request) {
    if (!isDevEnvironment()) {
      return notFound();
    }
    StorageUploadRequest signedRequest =
        storageClient.getSignedUploadRequest(
            "dev/${filename}", baseUrl + routes.FileUploadController.create().url());
    Set<StoredFile> files = storedFileRepository.list().toCompletableFuture().join();
    ImmutableList<StoredFile> fileList =
        files.stream()
            .sorted(Comparator.comparing(StoredFile::getName))
            .collect(ImmutableList.toImmutableList());
    return ok(view.render(request, signedRequest, fileList, request.flash().get("success")));
  }

  public Result create(Request request) {
    if (!isDevEnvironment()) {
      return notFound();
    }
    return cloudStorageStrategy.create(storedFileRepository, request);
  }
}
