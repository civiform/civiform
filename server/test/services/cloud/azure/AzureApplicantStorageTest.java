package services.cloud.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static services.cloud.azure.AzureApplicantStorage.AZURE_FILE_LIMIT_MB_CONF_PATH;

import com.typesafe.config.Config;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.cloud.StorageServiceName;

public class AzureApplicantStorageTest extends ResetPostgres {

  private static final String TEST_FILE_NAME = "fileName";
  private AzureApplicantStorage azureApplicantStorage;

  @Before
  public void setUp() {
    this.azureApplicantStorage = instanceOf(AzureApplicantStorage.class);
  }

  @Test
  public void getClient_instanceOfAzureBlobStorageClientForTest() {
    AzureBlobStorageClientInterface client = azureApplicantStorage.getClient();

    assertThat(client).isInstanceOf(TestAzureBlobStorageClient.class);
    assertThat(client).isInstanceOf(AzureBlobStorageClientInterface.class);
  }

  @Test
  public void getFileLimitMb() {
    int sizeLimit = azureApplicantStorage.getFileLimitMb();
    assertThat(sizeLimit)
        .isEqualTo(
            Integer.parseInt(instanceOf(Config.class).getString(AZURE_FILE_LIMIT_MB_CONF_PATH)));
  }

  @Test
  public void getBlobUrl() {
    String blobUrl = azureApplicantStorage.getClient().getBlobUrl(TEST_FILE_NAME);

    assertThat(blobUrl).isEqualTo("http://localhost");
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

    assertThat(url).isEqualTo("http://localhost?sasToken");
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
            .setBlobUrl("http://localhost")
            .setAccountName("my awesome azure account name")
            .setFileName(TEST_FILE_NAME)
            .setContainerName("super cool blob container name")
            .setSasToken("sasToken")
            .setSuccessActionRedirect("localhost")
            .setServiceName(StorageServiceName.AZURE_BLOB.getString())
            .build();

    BlobStorageUploadRequest blobStorageUploadRequest =
        azureApplicantStorage.getSignedUploadRequest(
            TEST_FILE_NAME, /* successActionRedirectUrl= */ "localhost");

    assertThat(blobStorageUploadRequest).isEqualTo(expectedRequest);
  }
}
