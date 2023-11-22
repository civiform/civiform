package services.cloud.azure;

import services.cloud.PublicStorageClient;
import services.cloud.StorageUploadRequest;

/** An Azure Blob Storage implementation of public storage. */
public class AzurePublicStorage implements PublicStorageClient {
  @Override
  public StorageUploadRequest getSignedUploadRequest(
    String fileKey, String successRedirectActionLink) {
    throw new UnsupportedOperationException("not implemented");
  }

  /** Returns a direct cloud storage URL to the file with the given key. */
  @Override
  public String getDisplayUrl(String fileKey) {
    throw new UnsupportedOperationException("not implemented");
  }
}
