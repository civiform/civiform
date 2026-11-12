package services.cloud.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

/**
 * Checks the permissions that the real Azure blob storage clients put in the SAS tokens they sign.
 * Download tokens must be read-only so that a link handed to someone who is allowed to read a file
 * can't be used to replace it. The Azure SDK is mocked, so no storage account or Azurite is needed.
 */
public class AzureBlobStorageClientSasPermissionTest {

  private static final String FILE_NAME = "applicant-1/program-2/block-3/file";
  private static final Config CONFIG =
      ConfigFactory.parseMap(
          Map.of(
              "base_url",
              "http://localhost:9000",
              "azure.local.connection",
              "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=a2V5;"
                  + "BlobEndpoint=http://azurite:10000/devstoreaccount1;"));

  private BlobClient blobClient;
  private BlobServiceClient blobServiceClient;

  @Before
  public void setUp() {
    blobClient = mock(BlobClient.class);
    BlobContainerClient containerClient = mock(BlobContainerClient.class);
    when(containerClient.getBlobClient(FILE_NAME)).thenReturn(blobClient);
    blobServiceClient = mock(BlobServiceClient.class);
    when(blobServiceClient.getBlobContainerClient("container")).thenReturn(containerClient);
  }

  @Test
  public void devClient_downloadToken_isReadOnly() {
    String permissions = devClientPermissions(/* download= */ true);

    assertThat(permissions).isEqualTo("r");
  }

  @Test
  public void devClient_uploadToken_canReadAndWrite() {
    String permissions = devClientPermissions(/* download= */ false);

    assertThat(permissions).isEqualTo("rw");
  }

  @Test
  public void deployedClient_downloadToken_isReadOnly() {
    String permissions = deployedClientPermissions(/* download= */ true);

    assertThat(permissions).isEqualTo("r");
  }

  @Test
  public void deployedClient_uploadToken_canReadAndWrite() {
    String permissions = deployedClientPermissions(/* download= */ false);

    assertThat(permissions).isEqualTo("rw");
  }

  private String devClientPermissions(boolean download) {
    // DevAzureBlobStorageClient builds its own BlobServiceClient, so swap in the mock.
    try (MockedConstruction<BlobServiceClientBuilder> ignored = mockBuilder()) {
      DevAzureBlobStorageClient client =
          new DevAzureBlobStorageClient(
              CONFIG,
              ZoneId.of("UTC"),
              "container",
              Duration.ofMinutes(10),
              /* allowPublicRead= */ false);
      if (download) {
        client.getDownloadSasToken(FILE_NAME, Optional.empty());
      } else {
        client.getSasToken(FILE_NAME, Optional.empty());
      }
    }
    ArgumentCaptor<BlobServiceSasSignatureValues> values =
        ArgumentCaptor.forClass(BlobServiceSasSignatureValues.class);
    verify(blobClient).generateSas(values.capture());
    return values.getValue().getPermissions();
  }

  private String deployedClientPermissions(boolean download) {
    UserDelegationKey key = mock(UserDelegationKey.class);
    when(key.getSignedExpiry()).thenReturn(OffsetDateTime.now(ZoneId.of("UTC")).plusDays(1));
    when(blobServiceClient.getUserDelegationKey(any(), any())).thenReturn(key);
    try (MockedConstruction<BlobServiceClientBuilder> ignored = mockBuilder()) {
      AzureBlobStorageClient client =
          new AzureBlobStorageClient(
              CONFIG,
              ZoneId.of("UTC"),
              mock(Credentials.class),
              "container",
              "https://example.blob.core.windows.net",
              Duration.ofMinutes(10),
              Duration.ofHours(1));
      if (download) {
        client.getDownloadSasToken(FILE_NAME, Optional.empty());
      } else {
        client.getSasToken(FILE_NAME, Optional.empty());
      }
    }
    ArgumentCaptor<BlobServiceSasSignatureValues> values =
        ArgumentCaptor.forClass(BlobServiceSasSignatureValues.class);
    verify(blobClient).generateUserDelegationSas(values.capture(), any(UserDelegationKey.class));
    return values.getValue().getPermissions();
  }

  /** Makes every {@code new BlobServiceClientBuilder()...buildClient()} return the mock client. */
  private MockedConstruction<BlobServiceClientBuilder> mockBuilder() {
    return mockConstruction(
        BlobServiceClientBuilder.class,
        withSettings().defaultAnswer(Answers.RETURNS_SELF),
        (builder, context) -> when(builder.buildClient()).thenReturn(blobServiceClient));
  }
}
