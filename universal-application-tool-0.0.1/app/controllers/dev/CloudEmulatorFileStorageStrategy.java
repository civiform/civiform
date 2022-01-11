package controllers.dev;

import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;

/**
 * Interface for interacting with the different cloud storage provider emulators in dev. The
 * interface methods are implemented for each cloud storage emulator and are then called by
 * dev/FileUploadController.
 */
public interface CloudEmulatorFileStorageStrategy {

  /**
   * Method for uploading a file and redirecting users' on success. Should be implemented for each
   * cloud storage provider.
   */
  Result create(StoredFileRepository storedFileRepository, Request request);
}
