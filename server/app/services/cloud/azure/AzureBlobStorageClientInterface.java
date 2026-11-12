package services.cloud.azure;

import java.util.Optional;

/** Interface defintion for Azure blob storage client. */
interface AzureBlobStorageClientInterface {

  /** Returns a SAS token for uploading a file. The token can read and write the blob. */
  String getSasToken(String fileName, Optional<String> originalFileName);

  /** Returns a SAS token for downloading a file. The token can only read the blob. */
  String getDownloadSasToken(String fileName, Optional<String> originalFileName);

  String getBlobUrl(String fileName);
}
