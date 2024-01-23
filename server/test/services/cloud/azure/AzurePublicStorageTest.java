package services.cloud.azure;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.common.collect.ImmutableSet;
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
