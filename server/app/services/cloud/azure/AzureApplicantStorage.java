package services.cloud.azure;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import java.net.URL;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.Environment;
import services.cloud.ApplicantStorageClient;
import services.cloud.StorageServiceName;

/** An Azure Blob Storage implementation of {@link ApplicantStorageClient}. */
@Singleton
public class AzureApplicantStorage implements ApplicantStorageClient {

  public static final String AZURE_STORAGE_ACCT_CONF_PATH = "azure.blob.account";
  public static final String AZURE_CONTAINER_NAME_CONF_PATH = "azure.blob.container_name";
  public static final Duration AZURE_SAS_TOKEN_DURATION = Duration.ofMinutes(10);

  // A User Delegation Key is used to sign SAS tokens without having to store the
  // Account Key alongside the application.The key needs to be rotated and this
  // duration is the interval at which it's rotated. More info here:
  // https://docs.microsoft.com/en-us/rest/api/storageservices/create-user-delegation-sas
  public static final Duration AZURE_USER_DELEGATION_KEY_DURATION = Duration.ofMinutes(60);

  private final String containerName;
  private final AzureBlobStorageClientInterface client;
  private final String accountName;

  @Inject
  public AzureApplicantStorage(
      Credentials credentials, Config config, Environment environment, ZoneId zoneId) {

    this.containerName = checkNotNull(config).getString(AZURE_CONTAINER_NAME_CONF_PATH);
    this.accountName = checkNotNull(config).getString(AZURE_STORAGE_ACCT_CONF_PATH);

    String blobEndpoint = String.format("https://%s.blob.core.windows.net", accountName);

    if (environment.isDev()) {
      client =
          new DevAzureBlobStorageClient(config, zoneId, containerName, AZURE_SAS_TOKEN_DURATION);
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
  public int getFileLimitMb() {
    // We currently don't enforce a file limit for Azure, so use the max integer value.
    // TODO(#7013): Enforce a file size limit for Azure.
    return Integer.MAX_VALUE;
  }

  @Override
  public String getPresignedUrlString(String fileKey) {
    return getPresignedUrlString(fileKey, /* prefixedOriginalFileName= */ Optional.empty());
  }

  @Override
  public String getPresignedUrlString(String fileKey, Optional<String> prefixedOriginalFileName) {
    String blobUrl = client.getBlobUrl(fileKey);
    String sasToken = client.getSasToken(fileKey, prefixedOriginalFileName);
    String signedUrl = String.format("%s?%s", blobUrl, sasToken);

    try {
      return new URL(signedUrl).toString();
    } catch (java.net.MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  AzureBlobStorageClientInterface getClient() {
    return client;
  }

  @Override
  public BlobStorageUploadRequest getSignedUploadRequest(
      String fileName, String successActionRedirectUrl) {
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
            .setSuccessActionRedirect(successActionRedirectUrl);
    return builder.build();
  }

  @Override
  public StorageServiceName getStorageServiceName() {
    return StorageServiceName.AZURE_BLOB;
  }
}
