package controllers.dev;

import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;

/**
 * Interface for interacting with the different cloud storage providers in dev. Each cloud storage
 * provider implements the interface methods, which can then be called by FileUploadController.
 */
public interface CloudStorageDevControllerStrategy {

  /**
   * Method for uploading a file and redirecting users' on success. Should be implemented for each
   * cloud storage provider.
   */
  Result create(StoredFileRepository storedFileRepository, Request request);
}
