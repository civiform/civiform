package services.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeApplication;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import play.Application;
import play.test.WithApplication;
import services.cloud.StorageServiceName;

public class SimpleStorageTest extends WithApplication {

  private static final ImmutableMap<String, Object> TEST_AWS_CONTAINER_CONFIG =
      ImmutableMap.of("aws.region", "us-west-2");

  private SimpleStorage simpleStorage;

  @Override
  protected Application provideApplication() {
    return fakeApplication(TEST_AWS_CONTAINER_CONFIG);
  }

  @Before
  public void setUp() {
    this.simpleStorage = instanceOf(SimpleStorage.class);
  }

  @Test
  public void getStorageServiceName() {
    StorageServiceName storageServiceName = simpleStorage.getStorageServiceName();

    assertThat(storageServiceName).isEqualTo(StorageServiceName.AWS_S3);
  }

  @Test
  public void getPresignedUrl() {
    String url = simpleStorage.getPresignedUrlString(/* key= */ "keyName");

    assertThat(url).isEqualTo("http://fake-url");
  }

  @Test
  public void getFileUploadRequest() {
    SignedS3UploadRequest actualUploadRequest =
        simpleStorage.getSignedUploadRequest("keyName", "successActionRedirect");

    assertThat(actualUploadRequest.accessKey()).isEqualTo("accessKeyId");
    assertThat(actualUploadRequest.regionName()).isEqualTo("us-west-2");
    assertThat(actualUploadRequest.actionLink()).isEqualTo("fake-bucket-address");
    assertThat(actualUploadRequest.successActionRedirect()).isEqualTo("successActionRedirect");
    assertThat(actualUploadRequest.bucket()).isEqualTo("civiform-local-s3");
    assertThat(actualUploadRequest.algorithm()).isEqualTo("AWS4-HMAC-SHA256");
  }
}
