package services.cloud.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static services.cloud.azure.AzurePublicStorage.AZURE_PUBLIC_CONTAINER_CONF_PATH;

import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.cloud.StorageServiceName;
import services.cloud.StorageUploadRequest;

public class AzurePublicStorageTest extends ResetPostgres {

  private static final String TEST_FILE_NAME = "fileName";
  private AzurePublicStorage azurePublicStorage;

  @Before
  public void setUp() {
    this.azurePublicStorage = instanceOf(AzurePublicStorage.class);
  }

  @Test
  public void getBucketName() {
    String bucket = azurePublicStorage.getBucketName();
    assertThat(bucket)
        .isEqualTo(instanceOf(Config.class).getString(AZURE_PUBLIC_CONTAINER_CONF_PATH));
  }

  @Test
  public void getFileLimitMb() {
    int sizeLimit = azurePublicStorage.getFileLimitMb();
    assertThat(sizeLimit).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  public void getSignedUploadRequest() {
    BlobStorageUploadRequest expectedRequest =
        BlobStorageUploadRequest.builder()
            .setBlobUrl("http://www.blobUrl.com")
            .setAccountName("my awesome azure account name")
            .setFileName(TEST_FILE_NAME)
            .setContainerName(instanceOf(Config.class).getString(AZURE_PUBLIC_CONTAINER_CONF_PATH))
            .setSasToken("sasToken")
            .setSuccessActionRedirect("www.redirectlink.com")
            .setServiceName(StorageServiceName.AZURE_BLOB.getString())
            .build();

    StorageUploadRequest blobStorageUploadRequest =
        azurePublicStorage.getSignedUploadRequest(
            TEST_FILE_NAME, /* successActionRedirectUrl= */ "www.redirectlink.com");

    assertThat(blobStorageUploadRequest).isEqualTo(expectedRequest);
  }

  @Test
  public void getPublicDisplayUrl_incorrectlyFormatted_throws() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> azurePublicStorage.getPublicDisplayUrl("fake-file-key"))
        .withMessageContaining("key incorrectly formatted");
  }

  @Test
  public void getPublicDisplayUrl_correctlyFormatted_throwsUnsupported() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(
            () ->
                azurePublicStorage.getPublicDisplayUrl(
                    "program-summary-image/program-10/myFile.jpeg"));
  }

  @Test
  public void prunePublicFileStorage_throwsUnsupported() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> azurePublicStorage.prunePublicFileStorage(ImmutableSet.of()));
  }
}
