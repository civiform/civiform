package services.cloud.azure;

import static com.google.common.base.Preconditions.checkNotNull;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.sas.SasProtocol;
import com.typesafe.config.Config;
import java.net.URLConnection;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

/** Class to use for deployed file uploads to Azure blob storage. */
class AzureBlobStorageClient extends BaseAzureBlobStorageClient {

  private final BlobServiceClient blobServiceClient;
  private UserDelegationKey userDelegationKey;
  private final ZoneId zoneId;
  private final Duration sasTokenDuration;
  private final Duration userDelegationKeyDuration;
  private final String containerName;

  AzureBlobStorageClient(
      Config config,
      ZoneId zoneId,
      Credentials credentials,
      String containerName,
      String blobEndpoint,
      Duration sasTokenDuration,
      Duration userDelegationKeyDuration) {
    this.zoneId = checkNotNull(zoneId);
    String baseUrl = checkNotNull(config).getString("base_url");
    this.containerName = checkNotNull(containerName);
    this.sasTokenDuration = checkNotNull(sasTokenDuration);
    this.userDelegationKeyDuration = checkNotNull(userDelegationKeyDuration);

    blobServiceClient =
        new BlobServiceClientBuilder()
            .endpoint(checkNotNull(blobEndpoint))
            .credential(checkNotNull(credentials).getCredentials())
            .buildClient();
    super.setCorsRules(blobServiceClient, baseUrl);
    userDelegationKey = getUserDelegationKey();
  }

  private UserDelegationKey getUserDelegationKey() {
    OffsetDateTime tokenExpiration = OffsetDateTime.now(zoneId).plus(sasTokenDuration);
    boolean isUserDelegationKeyExpired = false;
    if (userDelegationKey != null) {
      OffsetDateTime keyExpiration = userDelegationKey.getSignedExpiry();
      isUserDelegationKeyExpired = keyExpiration.isBefore(tokenExpiration);
    }
    if (userDelegationKey == null || isUserDelegationKeyExpired) {
      userDelegationKey =
          blobServiceClient.getUserDelegationKey(
              OffsetDateTime.now(zoneId).minus(Duration.ofMinutes(5)),
              OffsetDateTime.now(zoneId).plus(userDelegationKeyDuration));
    }
    return userDelegationKey;
  }

  @Override
  public String getSasToken(String fileName, Optional<String> prefixedOriginalFileName) {
    BlobClient blobClient =
        blobServiceClient.getBlobContainerClient(containerName).getBlobClient(fileName);

    BlobSasPermission blobSasPermission =
        new BlobSasPermission().setReadPermission(true).setWritePermission(true);

    BlobServiceSasSignatureValues signatureValues =
        new BlobServiceSasSignatureValues(
                OffsetDateTime.now(zoneId).plus(sasTokenDuration), blobSasPermission)
            .setProtocol(SasProtocol.HTTPS_ONLY);

    if (prefixedOriginalFileName.isPresent()) {
      signatureValues.setContentDisposition("inline; filename=" + prefixedOriginalFileName.get());
      signatureValues.setContentType(
          URLConnection.guessContentTypeFromName(prefixedOriginalFileName.get()));
    }

    return blobClient.generateUserDelegationSas(signatureValues, getUserDelegationKey());
  }

  @Override
  public String getBlobUrl(String fileName) {
    BlobClient blobClient =
        blobServiceClient.getBlobContainerClient(containerName).getBlobClient(fileName);
    return blobClient.getBlobUrl();
  }
}
