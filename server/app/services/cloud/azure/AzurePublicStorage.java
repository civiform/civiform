package services.cloud.azure;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import services.cloud.PublicStorageClient;
import services.cloud.StorageUploadRequest;

/**
 * An Azure Blob Storage implementation of public storage.
 *
 * <p>TODO(#9213): Complete program card image upload for azure.
 */
public class AzurePublicStorage extends PublicStorageClient {
  public static final String AZURE_STORAGE_ACCT_CONF_PATH = "azure.blob.account";
  public static final String AZURE_CONTAINER_CONF_PATH = "azure.blob.container";

  private final String container;
  private final Client client;
  private final String accountName;

  @Inject
  public AzurePublicStorage(Config config) {

    this.container = checkNotNull(config).getString(AZURE_CONTAINER_CONF_PATH);
    this.accountName = checkNotNull(config).getString(AZURE_STORAGE_ACCT_CONF_PATH);

    client = new NullClient();
  }

  @Override
  public String getBucketName() {
    return container;
  }

  @Override
  public int getFileLimitMb() {
    // We currently don't enforce a file limit for Azure, so use the max integer value.
    // TODO(#7013): Enforce a file size limit for Azure.
    return Integer.MAX_VALUE;
  }

  @Override
  public StorageUploadRequest getSignedUploadRequest(
      String fileKey, String successRedirectActionLink) {
    // Azure blob must know the name of a file to generate a SAS for it, so we'll
    // use a UUID When the file is uploaded, this UUID is stored along with the name
    // of the file.
    fileKey = fileKey.replace("${fileKey}", UUID.randomUUID().toString());

    BlobStorageUploadRequest.Builder builder =
        BlobStorageUploadRequest.builder()
            .setFileName(fileKey)
            .setAccountName(accountName)
            .setContainerName(container)
            .setBlobUrl(client.getBlobUrl(fileKey))
            .setSasToken(client.getSasToken(fileKey, Optional.empty()))
            .setSuccessActionRedirect(successRedirectActionLink);
    return builder.build();
  }

  @Override
  protected String getPublicDisplayUrlInternal(String fileKey) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void prunePublicFileStorage(ImmutableSet<String> inUseFileKeys) {
    throw new UnsupportedOperationException("not implemented");
  }

  interface Client {

    String getSasToken(String fileName, Optional<String> originalFileName);

    String getBlobUrl(String fileName);
  }

  /** Class to use for BlobStorage unit tests. */
  static class NullClient implements Client {

    NullClient() {}

    @Override
    public String getSasToken(String fileName, Optional<String> originalFileName) {
      if (originalFileName.isPresent()) {
        return "sasTokenWithContentHeaders";
      }
      return "sasToken";
    }

    @Override
    public String getBlobUrl(String fileName) {
      return "http://www.blobUrl.com";
    }
  }
}
