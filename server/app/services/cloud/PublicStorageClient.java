package services.cloud;

import com.google.common.collect.ImmutableSet;

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
   * Prunes the public file storage to only contain the files specified by {@code inUseFileKeys} and
   * nothing else. All other files in public storage will be removed.
   *
   * @param inUseFileKeys the set of file keys that are still being used and should *not* be deleted
   *     from storage.
   */
  public abstract void prunePublicFileStorage(ImmutableSet<String> inUseFileKeys);
}
