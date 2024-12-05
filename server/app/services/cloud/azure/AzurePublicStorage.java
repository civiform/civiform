package services.cloud.azure;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import play.Environment;
import services.cloud.PublicStorageClient;
import services.cloud.StorageUploadRequest;

/**
 * An Azure Blob Storage implementation of public storage.
 *
 * <p>TODO(#9213): Complete program card image upload for azure, as is, this is a no-op, test-only
 * implementation of the AzurePublicStorage class.
 */
public class AzurePublicStorage extends PublicStorageClient {
  /** NOOP -- REMOVE BEFORE MERGE -- */
  @VisibleForTesting
  static final String AZURE_PUBLIC_CONTAINER_NAME_CONF_PATH = "azure.blob.public_container_name";

  @VisibleForTesting
  static final String AZURE_PUBLIC_FILE_LIMIT_MB_CONF_PATH = "azure.blob.public_file_limit_mb";

  public static final String AZURE_STORAGE_ACCT_CONF_PATH = "azure.blob.account";
  public static final Duration AZURE_SAS_TOKEN_DURATION = Duration.ofMinutes(10);

  // A User Delegation Key is used to sign SAS tokens without having to store the
  // Account Key alongside the application.The key needs to be rotated and this
  // duration is the interval at which it's rotated. More info here:
  // https://docs.microsoft.com/en-us/rest/api/storageservices/create-user-delegation-sas
  public static final Duration AZURE_USER_DELEGATION_KEY_DURATION = Duration.ofMinutes(60);

  private final String containerName;
  private final String accountName;
  private final int fileLimitMb;
  private final AzureBlobStorageClientInterface client;

  @Inject
  public AzurePublicStorage(
      Credentials credentials, Config config, Environment environment, ZoneId zoneId) {

    this.containerName = checkNotNull(config).getString(AZURE_PUBLIC_CONTAINER_NAME_CONF_PATH);
    this.accountName = checkNotNull(config).getString(AZURE_STORAGE_ACCT_CONF_PATH);
    this.fileLimitMb = checkNotNull(config).getInt(AZURE_PUBLIC_FILE_LIMIT_MB_CONF_PATH);

    String blobEndpoint = String.format("https://%s.blob.core.windows.net", accountName);

    if (environment.isDev()) {
      client =
          new DevAzureBlobStorageClient(
              config, zoneId, containerName, AZURE_SAS_TOKEN_DURATION, /* allowPublicRead= */ true);
    } else if (environment.isTest()) {
      client = new TestAzureBlobStorageClient();
    } else {
      client =
          new AzureBlobStorageClient(
              config,
              zoneId,
              credentials,
              this.containerName,
              blobEndpoint,
              AZURE_SAS_TOKEN_DURATION,
              AZURE_USER_DELEGATION_KEY_DURATION);
    }
  }

  @Override
  public String getBucketName() {
    return containerName;
  }

  @Override
  public int getFileLimitMb() {
    return fileLimitMb;
  }

  @Override
  public StorageUploadRequest getSignedUploadRequest(
      String fileName, String successRedirectActionLink) {
    // Azure blob must know the name of a file to generate a SAS for it, so we'll
    // use a UUID When the file is uploaded, this UUID is stored along with the name
    // of the file.
    fileName = fileName.replace("${filename}", UUID.randomUUID().toString());

    BlobStorageUploadRequest.Builder builder =
        BlobStorageUploadRequest.builder()
            .setFileName(fileName)
            .setAccountName(accountName)
            .setContainerName(containerName)
            .setBlobUrl(client.getBlobUrl(fileName))
            .setSasToken(client.getSasToken(fileName, Optional.empty()))
            .setSuccessActionRedirect(successRedirectActionLink);
    return builder.build();
  }

  @Override
  protected String getPublicDisplayUrlInternal(String fileKey) {
    return client.getBlobUrl(fileKey);
  }

  @Override
  public void prunePublicFileStorage(ImmutableSet<String> inUseFileKeys) {
    throw new UnsupportedOperationException("not implemented");
  }
}
