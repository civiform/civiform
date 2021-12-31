package services.cloud;

import java.net.URL;
import models.StoredFile;

/** Interface for working with Cloud storage. We currently support AWS S3 and Azure Blob storage. */
public interface StorageClient {

  /**
   * Returns a URL that gives users temporary access to file storage. This URL is used to upload
   * users' files to cloud storage.
   *
   * @param file The file to be uploaded to cloud storage.
   */
  URL getPresignedUrl(StoredFile file);

  /**
   * Creates and returns a request to upload a file to cloud storage.
   *
   * @param fileName The file to upload to cloud storage
   * @param successRedirectActionLink Where a user should be redirected upon successful file upload.
   */
  StorageUploadRequest getSignedUploadRequest(String fileName, String successRedirectActionLink);

  /** Gets the {@link StorageServiceName} for the current storage client. */
  StorageServiceName getStorageServiceName();
}
