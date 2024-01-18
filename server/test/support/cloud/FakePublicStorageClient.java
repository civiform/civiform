package support.cloud;

import com.google.common.collect.ImmutableSet;
import services.cloud.PublicStorageClient;
import services.cloud.StorageUploadRequest;

/** A fake implementation of {@link PublicStorageClient} to be used in tests. */
public final class FakePublicStorageClient extends PublicStorageClient {

  private ImmutableSet<String> lastInUseFileKeys;

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
  public void prunePublicFileStorage(ImmutableSet<String> inUseFileKeys) {
    lastInUseFileKeys = inUseFileKeys;
  }

  /** Returns the last set of keys sent to {@link #prunePublicFileStorage}. */
  public ImmutableSet<String> getLastInUseFileKeys() {
    return lastInUseFileKeys;
  }
}
