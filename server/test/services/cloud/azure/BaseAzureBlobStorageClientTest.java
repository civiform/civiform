package services.cloud.azure;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.storage.blob.sas.BlobSasPermission;
import org.junit.Test;

public class BaseAzureBlobStorageClientTest {

  @Test
  public void downloadSasPermission_canReadButNotWrite() {
    BlobSasPermission permission = BaseAzureBlobStorageClient.downloadSasPermission();

    assertThat(permission.hasReadPermission()).isTrue();
    assertThat(permission.hasWritePermission()).isFalse();
    assertThat(permission.hasCreatePermission()).isFalse();
    assertThat(permission.hasDeletePermission()).isFalse();
  }

  @Test
  public void uploadSasPermission_canReadAndWrite() {
    BlobSasPermission permission = BaseAzureBlobStorageClient.uploadSasPermission();

    assertThat(permission.hasReadPermission()).isTrue();
    assertThat(permission.hasWritePermission()).isTrue();
  }
}
