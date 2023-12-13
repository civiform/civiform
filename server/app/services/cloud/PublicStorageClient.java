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
   * @param fileKey The name of the file used as a key in cloud storage.
   * @param successRedirectActionLink Where a user should be redirected upon successful file upload.
   */
  StorageUploadRequest getSignedUploadRequest(String fileKey, String successRedirectActionLink);

  /**
   * Returns a pubicly accessible URL to the file with the given key.
   *
   * <p>The URL is directly to the cloud storage provider and is **not** a CiviForm URL. This URL
   * will be used to display the file to anyone who accesses CiviForm.
   */
  String getPublicDisplayUrl(String fileKey);
}
