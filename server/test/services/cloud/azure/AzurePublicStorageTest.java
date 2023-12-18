package services.cloud.azure;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;

public class AzurePublicStorageTest extends ResetPostgres {

  private AzurePublicStorage azurePublicStorage;

  @Before
  public void setUp() {
    this.azurePublicStorage = instanceOf(AzurePublicStorage.class);
  }

  @Test
  public void getSignedUploadRequest_throwsUnsupported() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> azurePublicStorage.getSignedUploadRequest("fileKey", "successRedirect"));
  }

  @Test
  public void getPublicDisplayUrl_throwsUnsupported() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> azurePublicStorage.getPublicDisplayUrl("fileKey"));
  }
}
