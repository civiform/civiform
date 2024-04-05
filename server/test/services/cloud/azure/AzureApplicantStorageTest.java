package services.cloud.azure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.cloud.StorageServiceName;
import services.cloud.azure.AzureApplicantStorage.Client;
import services.cloud.azure.AzureApplicantStorage.NullClient;

public class AzureApplicantStorageTest extends ResetPostgres {

  private static final String TEST_FILE_NAME = "fileName";
  private AzureApplicantStorage azureApplicantStorage;

  @Before
  public void setUp() {
    this.azureApplicantStorage = instanceOf(AzureApplicantStorage.class);
  }

  @Test
  public void getClient_instanceOfNullClient() {
    Client client = azureApplicantStorage.getClient();

    assertThat(client).isInstanceOf(NullClient.class);
    assertThat(client).isInstanceOf(Client.class);
  }

  @Test
  public void getBlobUrl() {
    String blobUrl = azureApplicantStorage.getClient().getBlobUrl(TEST_FILE_NAME);

    assertThat(blobUrl).isEqualTo("http://www.blobUrl.com");
  }

  @Test
  public void getSasToken_originalFileNameNotSet() {
    String sasToken =
        azureApplicantStorage.getClient().getSasToken(TEST_FILE_NAME, Optional.empty());

    assertThat(sasToken).isEqualTo("sasToken");
  }

  @Test
  public void getSasToken_originalFileNameSet() {
    String sasToken =
        azureApplicantStorage.getClient().getSasToken(TEST_FILE_NAME, Optional.of("file.pdf"));
    assertThat(sasToken).isEqualTo("sasTokenWithContentHeaders");
  }

  @Test
  public void getPresignedUrl() {
    String url = azureApplicantStorage.getPresignedUrlString(TEST_FILE_NAME, Optional.empty());

    assertThat(url).isEqualTo("http://www.blobUrl.com?sasToken");
  }

  @Test
  public void getStorageServiceName() {
    StorageServiceName storageServiceName = azureApplicantStorage.getStorageServiceName();

    assertThat(storageServiceName).isEqualTo(StorageServiceName.AZURE_BLOB);
  }

  @Test
  public void getFileUploadRequest() {
    BlobStorageUploadRequest expectedRequest =
        BlobStorageUploadRequest.builder()
            .setBlobUrl("http://www.blobUrl.com")
            .setAccountName("my awesome azure account name")
            .setFileName(TEST_FILE_NAME)
            .setContainerName("super cool blob container name")
            .setSasToken("sasToken")
            .setSuccessActionRedirect("www.redirectlink.com")
            .setServiceName(StorageServiceName.AZURE_BLOB.getString())
            .build();

    BlobStorageUploadRequest blobStorageUploadRequest =
        azureApplicantStorage.getSignedUploadRequest(
            TEST_FILE_NAME, /* successActionRedirectUrl= */ "www.redirectlink.com");

    assertThat(blobStorageUploadRequest).isEqualTo(expectedRequest);
  }
}
