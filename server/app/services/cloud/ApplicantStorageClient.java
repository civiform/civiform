package services.cloud;

import java.util.Optional;

/**
 * Interface for working with cloud file storage backends for applicant files. This:
 *
 * <p>(1) Allows applicants to upload files as part of their application -- see {@link
 * #getSignedUploadRequest(String, String)}.
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
   * is used to access and download the users' files from cloud storage. The
   * prefixedOriginalFileName will either be in the format "dev/${filename}" or
   * applicant-%d/program-%d/block-%s/${filename}" where "${filename}" is the name of the uploaded
   * file which is set by a user. For more information on prefixed filenames, see {@link
   * ApplicantFileNameFormatter}
   *
   * @param fileKey The file key to be accessed from cloud storage.
   * @param prefixedOriginalFileName The file name set by the user (optional).
   */
  String getPresignedUrlString(String fileKey, Optional<String> prefixedOriginalFileName);

  /**
   * Creates and returns a request to upload a file to cloud storage.
   *
   * @param fileKey The file key to use when uploading to cloud storage
   * @param successActionRedirect Where a user should be redirected upon successful file upload.
   */
  StorageUploadRequest getSignedUploadRequest(String fileKey, String successActionRedirect);

  /** Gets the {@link StorageServiceName} for the current storage client. */
  StorageServiceName getStorageServiceName();
}
