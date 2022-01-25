package services.cloud;

import java.util.Optional;

/** Interface for working with cloud file storage backends. */
public interface StorageClient {

  /**
   * Returns the string version of a URL that gives users temporary access to file storage. This URL
   * is used to access and download users' files from cloud storage. This calls the follow
   * getPresignedUrlString method with an empty Optional for the prefixed original file name.
   *
   * @param fileKey The file key to be accessed from cloud storage.
   */
  String getPresignedUrlString(String fileKey);

  /**
   * Returns the string version of a URL that gives users temporary access to file storage. This URL
   * is used to access and download users' files from cloud storage. The prefixedOriginalFileName
   * will either be in the format "dev/${filename}" or applicant-%d/program-%d/block-%s/${filename}"
   * where "${filename}" is the name of the uploaded file which is set by a user. For more
   * information on prefixed filenames, see {@link services.cloud.FileNameFormatter}
   *
   * @param fileKey The file key to be accessed from cloud storage.
   * @param prefixedOriginalFileName The file name set by the user (optional).
   */
  String getPresignedUrlString(String fileKey, Optional<String> prefixedOriginalFileName);

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
