package services.cloud.azure;

import services.cloud.PublicStorageClient;
import services.cloud.StorageUploadRequest;

/** An Azure Blob Storage implementation of public storage. */
public class AzurePublicStorage extends PublicStorageClient {
  @Override
  public StorageUploadRequest getSignedUploadRequest(
      String fileKey, String successRedirectActionLink) {
    // TODO(#5676): Implement for Azure.
    throw new UnsupportedOperationException("not implemented");
  }

  /** Returns a direct cloud storage URL to the file with the given key. */
  @Override
  protected String getPublicDisplayUrlInternal(String fileKey) {
    // TODO(#5676): Implement for Azure.
    throw new UnsupportedOperationException("not implemented");
  }
}
