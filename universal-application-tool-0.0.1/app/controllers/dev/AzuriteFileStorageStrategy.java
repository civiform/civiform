package controllers.dev;

import static play.mvc.Results.redirect;

import java.util.Optional;
import models.StoredFile;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;
import services.cloud.FileNameFormatter;

/** Implements file uploading to Azurite, the Azure emulator. */
public class AzuriteFileStorageStrategy implements CloudEmulatorFileStorageStrategy {

  @Override
  public Result create(StoredFileRepository storedFileRepository, Request request) {
    Optional<String> etag = request.queryString("etag");
    Optional<String> container = request.queryString("bucket");
    Optional<String> fileName = request.queryString("key");
    Optional<String> originalFileName = request.queryString("originalFileName");
    updateFileRecord(storedFileRepository, fileName.get(), originalFileName.get());
    String successMessage =
        String.format(
            "File successfully uploaded to Azure: container: %s, file name: %s, etag: %s, user"
                + " file name: %s.",
            container.get(), fileName.get(), etag.orElse(""), originalFileName.get());
    return redirect(routes.FileUploadController.index().url()).flashing("success", successMessage);
  }

  private void updateFileRecord(
      StoredFileRepository storedFileRepository, String key, String originalFileName) {
    StoredFile storedFile = new StoredFile();
    storedFile.setName(key);
    storedFile.setOriginalFileName(
        FileNameFormatter.getPrefixedOriginalFileName(key, originalFileName));
    storedFileRepository.insert(storedFile);
  }
}
