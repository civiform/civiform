package services.cloud;

/**
 * Interface for working with cloud storage file backends for **publicly accessible** files.
 *
 * <p>This should **never** be used to store any applicant data. Use {@link ApplicantStorageClient}
 * instead.
 */
public interface PublicStorageClient {
  /**
   * Creates and returns a request to upload a **publicly accessible** file to cloud storage.
   *
   * @param fileKey The name of the file used as a key in cloud storage
   * @param successRedirectActionLink Where a user should be redirected upon successful file upload.
   */
  StorageUploadRequest getSignedUploadRequest(String fileKey, String successRedirectActionLink);

  /** Returns a direct cloud storage URL to the file with the given key. */
  String getDisplayUrl(String fileKey);
}
