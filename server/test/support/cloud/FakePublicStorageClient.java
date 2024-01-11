package support.cloud;

import services.cloud.PublicStorageClient;
import services.cloud.StorageUploadRequest;

/** A fake implementation of {@link PublicStorageClient} to be used in tests. */
public final class FakePublicStorageClient extends PublicStorageClient {
  private boolean shouldDeleteSuccessfully = true;
  private String lastDeletedFileKey;

  public FakePublicStorageClient() {}

  @Override
  public StorageUploadRequest getSignedUploadRequest(
      String fileKey, String successRedirectActionLink) {
    return () -> "fakeServiceName";
  }

  @Override
  protected String getPublicDisplayUrlInternal(String fileKey) {
    return "fakeUrl.com/" + fileKey;
  }

  @Override
  protected boolean deletePublicFileInternal(String fileKey) {
    lastDeletedFileKey = fileKey;
    return shouldDeleteSuccessfully;
  }

  /** Sets whether this storage client should delete files successfully or fail to delete them. */
  public void setShouldDeleteSuccessfully(boolean shouldDeleteSuccessfully) {
    this.shouldDeleteSuccessfully = shouldDeleteSuccessfully;
  }

  public String getLastDeletedFileKey() {
    return lastDeletedFileKey;
  }
}
