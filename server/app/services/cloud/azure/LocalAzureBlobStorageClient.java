package services.cloud.azure;

import static com.google.common.base.Preconditions.checkNotNull;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.sas.SasProtocol;
import com.typesafe.config.Config;
import java.net.URLConnection;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Class to use for Azure blob storage in a dev environment.
 *
 * <p>This client communicates with the Azurite simulator.
 */
class LocalAzureBlobStorageClient extends BaseAzureBlobStorageClient {
  // Using Account SAS because Azurite emulator does not support User Delegation
  // SAS yet See https://github.com/Azure/Azurite/issues/656

  private static final String AZURE_LOCAL_CONNECTION_STRING_CONF_PATH = "azure.local.connection";

  private final String connectionString;
  private final BlobServiceClient blobServiceClient;
  private final BlobContainerClient blobContainerClient;
  private final ZoneId zoneId;
  private final Duration sasTokenDuration;

  LocalAzureBlobStorageClient(
      Config config, ZoneId zoneId, String containerName, Duration sasTokenDuration) {
    this.zoneId = checkNotNull(zoneId);
    this.sasTokenDuration = checkNotNull(sasTokenDuration);
    this.connectionString = checkNotNull(config).getString(AZURE_LOCAL_CONNECTION_STRING_CONF_PATH);
    this.blobServiceClient =
        new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
    String baseUrl = checkNotNull(config).getString("base_url");
    super.setCorsRules(blobServiceClient, baseUrl);
    this.blobContainerClient =
        blobServiceClient.getBlobContainerClient(checkNotNull(containerName));
    if (!blobContainerClient.exists()) {
      blobContainerClient.create();
    }
  }

  @Override
  public String getSasToken(String fileName, Optional<String> prefixedOriginalFileName) {
    BlobClient blobClient = blobContainerClient.getBlobClient(fileName);

    BlobSasPermission blobSasPermission =
        new BlobSasPermission().setReadPermission(true).setWritePermission(true);

    BlobServiceSasSignatureValues signatureValues =
        new BlobServiceSasSignatureValues(
                OffsetDateTime.now(zoneId).plus(sasTokenDuration), blobSasPermission)
            .setProtocol(SasProtocol.HTTPS_HTTP);

    if (prefixedOriginalFileName.isPresent()) {
      signatureValues.setContentDisposition("inline; filename=" + prefixedOriginalFileName.get());
      signatureValues.setContentType(
          URLConnection.guessContentTypeFromName(prefixedOriginalFileName.get()));
    }

    return blobClient.generateSas(signatureValues);
  }

  @Override
  public String getBlobUrl(String fileName) {
    return blobContainerClient.getBlobClient(fileName).getBlobUrl();
  }
}
