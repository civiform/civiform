package services.cloud;

import java.net.URL;

/** Interface for working with Cloud storage. We currently support AWS S3 and Azure Blob storage. */
public interface StorageClient {

  URL getPresignedUrl(String fileName);

  StorageUploadRequest getSignedUploadRequest(String fileName, String successRedirectActionLink);

  /** Gets the {@link StorageServiceName} for the current storage client. */
  StorageServiceName getStorageServiceName();
}
