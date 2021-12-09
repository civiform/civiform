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
import services.cloud.aws.SignedS3UploadRequest;
import services.cloud.aws.SimpleStorage;
import views.dev.FileUploadView;

/** Controller for interacting with S3 directly in dev mode. */
public class FileUploadController extends DevController {
  private final FileUploadView view;
  private final SimpleStorage s3Client;
  private final StoredFileRepository storedFileRepository;
  private final String baseUrl;

  @Inject
  public FileUploadController(
      FileUploadView view,
      SimpleStorage s3Client,
      StoredFileRepository storedFileRepository,
      Environment environment,
      Config configuration) {
    super(environment, configuration);
    this.view = checkNotNull(view);
    this.s3Client = checkNotNull(s3Client);
    this.storedFileRepository = checkNotNull(storedFileRepository);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
  }

  public Result index(Request request) {
    if (!isDevEnvironment()) {
      return notFound();
    }
    SignedS3UploadRequest signedRequest =
        s3Client.getSignedUploadRequest(
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
    Optional<String> bucket = request.queryString("bucket");
    Optional<String> key = request.queryString("key");
    Optional<String> etag = request.queryString("etag");
    if (!bucket.isPresent() || !key.isPresent()) {
      return redirect(routes.FileUploadController.index().url());
    }
    updateFileRecord(key.get());
    String successMessage =
        String.format(
            "File successfully uploaded to S3: bucket: %s, key: %s, etag: %s.",
            bucket.get(), key.get(), etag.orElse(""));
    return redirect(routes.FileUploadController.index().url()).flashing("success", successMessage);
  }

  private void updateFileRecord(String key) {
    StoredFile storedFile = new StoredFile();
    storedFile.setName(key);
    storedFileRepository.insert(storedFile);
  }
}
