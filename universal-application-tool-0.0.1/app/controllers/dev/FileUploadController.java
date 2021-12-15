package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import models.StoredFile;
import play.Environment;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;
import services.cloud.StorageClient;
import services.cloud.StorageServiceName;
import services.cloud.StorageUploadRequest;
import views.dev.FileUploadView;

/**
 * Controller for interacting with S3 directly in dev mode.
 */
public class FileUploadController extends DevController {

  private final FileUploadView view;
  private final StorageClient storageClient;
  private final StoredFileRepository storedFileRepository;
  private final String baseUrl;

  @Inject
  public FileUploadController(
      FileUploadView view,
      StorageClient storageClient,
      StoredFileRepository storedFileRepository,
      Environment environment,
      Config configuration) {
    super(environment, configuration);
    this.view = checkNotNull(view);
    this.storageClient = checkNotNull(storageClient);
    this.storedFileRepository = checkNotNull(storedFileRepository);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
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
    Optional<String> etag = request.queryString("etag");
    String successMessage;
    if (storageClient.getStorageServiceName() == StorageServiceName.AWS_S3) {
      Optional<String> bucket = request.queryString("bucket");
      Optional<String> key = request.queryString("key");
      if (!bucket.isPresent() || !key.isPresent()) {
        return redirect(routes.FileUploadController.index().url());
      }
      updateFileRecord(key.get());
      successMessage =
          String.format(
              "File successfully uploaded to S3: bucket: %s, key: %s, etag: %s.",
              bucket.get(), key.get(), etag.orElse(""));
    } else {
      Optional<String> container = request.queryString("container");
      Optional<String> fileName = request.queryString("fileName");
      Optional<String> userFileName = request.queryString("userFileName");
      updateFileRecord(fileName.get());
      successMessage =
          String.format(
              "File successfully uploaded to Azure: container: %s, file name: %s, etag: %s, user"
                  + " file name: %s.",
              container.get(), fileName.get(), etag.orElse(""), userFileName.get());
    }
    return redirect(routes.FileUploadController.index().url()).flashing("success", successMessage);
  }

  private void updateFileRecord(String key) {
    StoredFile storedFile = new StoredFile();
    storedFile.setName(key);
    storedFileRepository.insert(storedFile);
  }
}
