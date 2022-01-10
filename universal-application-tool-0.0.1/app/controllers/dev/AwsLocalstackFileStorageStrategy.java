package controllers.dev;

import static play.mvc.Results.redirect;

import java.util.Optional;
import models.StoredFile;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;

/** Implements file uploading for AWS cloud storage. Redirects users on success. */
public class AwsLocalstackFileStorageStrategy implements CloudEmulatorFileStorageStrategy {

  @Override
  public Result create(StoredFileRepository storedFileRepository, Request request) {
    Optional<String> etag = request.queryString("etag");
    Optional<String> bucket = request.queryString("bucket");
    Optional<String> key = request.queryString("key");
    if (!bucket.isPresent() || !key.isPresent()) {
      return redirect(routes.FileUploadController.index().url());
    }
    updateFileRecord(storedFileRepository, key.get());
    String successMessage =
        String.format(
            "File successfully uploaded to S3: bucket: %s, key: %s, etag: %s.",
            bucket.get(), key.get(), etag.orElse(""));
    return redirect(routes.FileUploadController.index().url()).flashing("success", successMessage);
  }

  private void updateFileRecord(StoredFileRepository storedFileRepository, String key) {
    StoredFile storedFile = new StoredFile();
    storedFile.setName(key);
    storedFileRepository.insert(storedFile);
  }
}
