package services.cloud.azure;

import com.google.common.collect.ImmutableSet;
import services.cloud.PublicStorageClient;
import services.cloud.StorageUploadRequest;

/** An Azure Blob Storage implementation of public storage. */
public class AzurePublicStorage extends PublicStorageClient {
  @Override
  public String getBucketName() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public int getFileLimitMb() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public StorageUploadRequest getSignedUploadRequest(
      String fileKey, String successRedirectActionLink) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  protected String getPublicDisplayUrlInternal(String fileKey) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void prunePublicFileStorage(ImmutableSet<String> inUseFileKeys) {
    throw new UnsupportedOperationException("not implemented");
  }
}
