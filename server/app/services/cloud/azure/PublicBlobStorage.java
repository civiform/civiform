package services.cloud.azure;

import services.cloud.PublicStorageClient;
import services.cloud.StorageUploadRequest;

public final class PublicBlobStorage implements PublicStorageClient {
  @Override
  public StorageUploadRequest getSignedUploadRequest(String fileName, String successRedirectActionLink) {
    // TODO: Implement
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public String getDisplayUrl(String fileKey) {
    // TODO: Implement
    throw new IllegalStateException("Not implemented");
  }
}
