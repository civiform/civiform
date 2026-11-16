package services.cloud.azure;

import java.util.Optional;

/** Class to use for BlobStorage unit tests. */
class TestAzureBlobStorageClient implements AzureBlobStorageClientInterface {

  TestAzureBlobStorageClient() {}

  @Override
  public String getSasToken(String fileName, Optional<String> originalFileName) {
    if (originalFileName.isPresent()) {
      return "sasTokenWithContentHeaders";
    }
    return "sasToken";
  }

  @Override
  public String getDownloadSasToken(String fileName, Optional<String> originalFileName) {
    if (originalFileName.isPresent()) {
      return "downloadSasTokenWithContentHeaders";
    }
    return "downloadSasToken";
  }

  @Override
  public String getBlobUrl(String fileName) {
    return "http://localhost";
  }
}
