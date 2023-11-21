package services.cloud;

import java.net.URL;

/**
 * Interface for uploading files that will be publicly accessible to cloud storage backends.
 *
 * See {@link StorageClient} for uploading applicant files.
 */
public interface PublicStorageClient {
  /**
   * Creates and returns a request to upload a file to cloud storage.
   *
   * @param fileName The file to upload to cloud storage
   * @param successRedirectActionLink Where a user should be redirected upon successful file upload.
   */
  StorageUploadRequest getSignedUploadRequest(String fileName, String successRedirectActionLink);

  String getDisplayUrl(String fileKey);
}
