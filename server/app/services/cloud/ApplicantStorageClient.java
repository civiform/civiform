package services.cloud;

import java.util.Optional;

/**
 * Interface for working with cloud file storage backends for applicant files. This:
 *
 * <p>(1) Allows applicants to upload files as part of their application -- see {@link
 * #getSignedUploadRequest}.
 *
 * <p>(2) Allows applicants to re-download files they've previously uploaded -- see {@link
 * #getPresignedUrlString(String)}.
 *
 * <p>(3) Allows program admins to view applicant-uploaded files only for programs they have
 * permissions to view -- see {@link #getPresignedUrlString(String)}. {@link
 * controllers.FileController} is responsible for checking the file ACLs before allowing access to
 * the files.
 */
public interface ApplicantStorageClient {
  /** Returns the maximum file size in megabytes allowed for public files. */
  int getFileLimitMb();

  /**
   * Returns the string version of a URL that gives users temporary access to file storage. This URL
   * is used to access and download the users' files from cloud storage. This calls {@link
   * #getPresignedUrlString(String, Optional)} (below) with an empty Optional.
   *
   * @param fileKey The file key to be accessed from cloud storage.
   */
  String getPresignedUrlString(String fileKey);

  /**
   * Returns the string version of a URL that gives users temporary access to file storage. This URL
   * is used to access and download the users' files from cloud storage.
   *
   * <p>If {@code originalFileName} is present, implementations set the {@code Content-Disposition}
   * response header so clients use that name when opening or saving the object. The value is either
   * the applicant-facing name from {@link models.StoredFileModel} or a basename derived from {@code
   * fileKey}. The object to fetch is always identified by {@code fileKey}.
   *
   * @param fileKey The file key to be accessed from cloud storage.
   * @param originalFileName Download filename for the client.
   */
  String getPresignedUrlString(String fileKey, Optional<String> originalFileName);

  /**
   * Creates and returns a request to upload a file to cloud storage.
   *
   * @param fileKey The file key to use when uploading to cloud storage
   * @param successActionRedirectUrl a URL specifying where a user should be redirected upon
   *     successful file upload.
   */
  StorageUploadRequest getSignedUploadRequest(String fileKey, String successActionRedirectUrl);

  /** Gets the {@link StorageServiceName} for the current storage client. */
  StorageServiceName getStorageServiceName();
}
