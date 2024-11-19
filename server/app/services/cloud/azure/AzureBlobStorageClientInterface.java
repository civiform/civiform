package services.cloud.azure;

import java.util.Optional;

/** Interface defintion for Azure blob storage client. */
interface AzureBlobStorageClientInterface {

  String getSasToken(String fileName, Optional<String> originalFileName);

  String getBlobUrl(String fileName);
}
