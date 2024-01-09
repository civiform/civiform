package services.cloud;

/**
 * Interface for working with cloud storage file backends for **publicly accessible** files.
 *
 * <p>This should **never** be used to store any applicant data. Use {@link ApplicantStorageClient}
 * instead.
 */
public abstract class PublicStorageClient {
  /**
   * Creates and returns a request to upload a **publicly accessible** file to cloud storage.
   *
   * @param fileKey The name of the file used as a key in cloud storage.
   * @param successRedirectActionLink Where a user should be redirected upon successful file upload.
   */
  public abstract StorageUploadRequest getSignedUploadRequest(
      String fileKey, String successRedirectActionLink);

  /**
   * Returns a publicly accessible URL to the file with the given key.
   *
   * <p>The URL is directly to the cloud storage provider and is **not** a CiviForm URL. This URL
   * will be used to display the file to anyone who accesses CiviForm.
   *
   * @throws IllegalArgumentException if the file key doesn't represent a file that should be
   *     publicly accessible.
   */
  public final String getPublicDisplayUrl(String fileKey) {
    if (!PublicFileNameFormatter.isFileKeyForPublicProgramImage(fileKey)) {
      throw new IllegalArgumentException("File key incorrectly formatted for public use");
    }
    return getPublicDisplayUrlInternal(fileKey);
  }

  /**
   * DO NOT CALL THIS METHOD - call {@link #getPublicDisplayUrl(String)} instead.
   *
   * <p>Purposefully not public so that all clients use {@link #getPublicDisplayUrl(String)}.
   */
  protected abstract String getPublicDisplayUrlInternal(String fileKey);

  /**
   * Removes the file specified by the given key from cloud storage.
   *
   * @return true if the file was successfully deleted and false otherwise.
   *
   * @throws IllegalArgumentException if the file key doesn't represent a file that's
   *     publicly accessible. This is to protect against accidentally deleting an applicant file.
   **/
  public final boolean deletePublicFile(String fileKey) {
    if (!PublicFileNameFormatter.isFileKeyForPublicProgramImage(fileKey)) {
      throw new IllegalArgumentException("File key incorrectly formatted for public use, so cannot be deleted");
    }
    return deletePublicFileInternal(fileKey);
  }

  /**
   * DO NOT CALL THIS METHOD - call {@link #deletePublicFile(String)} instead.
   *
   * <p>Purposefully not public so that all clients use {@link #deletePublicFile(String)}.
   */
  protected abstract boolean deletePublicFileInternal(String fileKey);
}
