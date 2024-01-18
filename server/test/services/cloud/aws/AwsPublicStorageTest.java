package services.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import java.io.File;
import java.net.URI;
import org.junit.Test;
import play.Environment;
import play.Mode;
import repository.ResetPostgres;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import support.cloud.FakeAwsS3Client;

public class AwsPublicStorageTest extends ResetPostgres {

  private final FakeAwsS3Client fakeAwsS3Client = new FakeAwsS3Client();

  @Test
  public void getSignedUploadRequest_prodEnv_actionLinkIsProdAws() {
    AwsPublicStorage awsPublicStorage =
        new AwsPublicStorage(
            fakeAwsS3Client,
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            instanceOf(Credentials.class),
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.PROD));

    SignedS3UploadRequest uploadRequest =
        awsPublicStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.actionLink()).contains("amazonaws.com");
    assertThat(uploadRequest.actionLink()).endsWith("/");
  }

  @Test
  public void getSignedUploadRequest_devEnv_actionLinkIsLocalStack() {
    AwsPublicStorage awsPublicStorage =
        new AwsPublicStorage(
            fakeAwsS3Client,
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            instanceOf(Credentials.class),
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.DEV));

    SignedS3UploadRequest uploadRequest =
        awsPublicStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.actionLink()).contains("localstack");
    assertThat(uploadRequest.actionLink()).doesNotContain("amazonaws.com");
    assertThat(uploadRequest.actionLink()).endsWith("/");
  }

  @Test
  public void getSignedUploadRequest_testEnv_notLocalStackOrProdAws() {
    AwsPublicStorage awsPublicStorage =
        new AwsPublicStorage(
            fakeAwsS3Client,
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            instanceOf(Credentials.class),
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.TEST));

    SignedS3UploadRequest uploadRequest =
        awsPublicStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.actionLink()).doesNotContain("localstack");
    assertThat(uploadRequest.actionLink()).doesNotContain("amazonaws.com");
  }

  @Test
  public void getSignedUploadRequest_hasCredentialsAndRegion() {
    AwsRegion region = instanceOf(AwsRegion.class);
    Credentials credentials = instanceOf(Credentials.class);
    AwsPublicStorage awsPublicStorage =
        new AwsPublicStorage(
            fakeAwsS3Client,
            instanceOf(AwsStorageUtils.class),
            region,
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class));

    SignedS3UploadRequest uploadRequest =
        awsPublicStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.accessKey()).isEqualTo(credentials.getCredentials().accessKeyId());
    assertThat(uploadRequest.secretKey()).isEqualTo(credentials.getCredentials().secretAccessKey());
    assertThat(uploadRequest.regionName()).isEqualTo(region.get().id());
  }

  @Test
  public void getSignedUploadRequest_hasPublicBucket() {
    AwsPublicStorage awsPublicStorage = instanceOf(AwsPublicStorage.class);

    SignedS3UploadRequest uploadRequest =
        awsPublicStorage.getSignedUploadRequest("test/fake/fakeFile.png", "redirect");

    assertThat(uploadRequest.bucket()).isEqualTo("civiform-local-s3-public");
  }

  @Test
  public void getSignedUploadRequest_hasPublicFileLimit() {
    AwsPublicStorage awsPublicStorage = instanceOf(AwsPublicStorage.class);

    SignedS3UploadRequest uploadRequest =
        awsPublicStorage.getSignedUploadRequest("test/fake/fakeFile.png", "redirect");

    assertThat(uploadRequest.fileLimitMb()).isEqualTo(1);
  }

  @Test
  public void getSignedUploadRequest_hasFileKey() {
    AwsPublicStorage awsPublicStorage = instanceOf(AwsPublicStorage.class);

    SignedS3UploadRequest uploadRequest =
        awsPublicStorage.getSignedUploadRequest("test/fake/fakeFile.png", "redirect");

    assertThat(uploadRequest.key()).isEqualTo("test/fake/fakeFile.png");
  }

  @Test
  public void getSignedUploadRequest_hasSuccessRedirect() {
    AwsPublicStorage awsPublicStorage = instanceOf(AwsPublicStorage.class);

    SignedS3UploadRequest uploadRequest =
        awsPublicStorage.getSignedUploadRequest("fileKey", "http://redirect.to.here");

    assertThat(uploadRequest.successActionRedirect()).isEqualTo("http://redirect.to.here");
  }

  @Test
  public void getSignedUploadRequest_sessionCredentials_hasSecurityToken() {
    Credentials credentials = mock(Credentials.class);
    AwsSessionCredentials sessionCredentials = mock(AwsSessionCredentials.class);
    when(sessionCredentials.accessKeyId()).thenReturn("accessKeyId");
    when(sessionCredentials.secretAccessKey()).thenReturn("secretKey");

    when(credentials.getCredentials()).thenReturn(sessionCredentials);
    when(sessionCredentials.sessionToken()).thenReturn("testSessionToken");
    AwsPublicStorage awsPublicStorage =
        new AwsPublicStorage(
            fakeAwsS3Client,
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class));

    SignedS3UploadRequest uploadRequest =
        awsPublicStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.securityToken()).isEqualTo("testSessionToken");
  }

  @Test
  public void getSignedUploadRequest_noSessionCredentials_noSecurityToken() {
    Credentials credentials = mock(Credentials.class);
    AwsCredentials notSessionCredentials = mock(AwsCredentials.class);
    when(notSessionCredentials.accessKeyId()).thenReturn("accessKeyId");
    when(notSessionCredentials.secretAccessKey()).thenReturn("secretKey");
    when(credentials.getCredentials()).thenReturn(notSessionCredentials);

    AwsPublicStorage awsPublicStorage =
        new AwsPublicStorage(
            fakeAwsS3Client,
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class));

    SignedS3UploadRequest uploadRequest =
        awsPublicStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.securityToken()).isEmpty();
  }

  @Test
  public void getPublicDisplayUrl_incorrectlyFormatted_throws() {
    AwsPublicStorage awsPublicStorage = instanceOf(AwsPublicStorage.class);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> awsPublicStorage.getPublicDisplayUrl("fake-file-key"))
        .withMessageContaining("key incorrectly formatted");
  }

  @Test
  public void getPublicDisplayUrl_correctlyFormatted_hasActionLinkAndFileKey() {
    AwsPublicStorage awsPublicStorage = instanceOf(AwsPublicStorage.class);

    String publicDisplayUrl =
        awsPublicStorage.getPublicDisplayUrl("program-summary-image/program-10/myFile.jpeg");

    assertThat(publicDisplayUrl)
        .isEqualTo("fake-action-link/program-summary-image/program-10/myFile.jpeg");
  }

  @Test
  public void prunePublicFileStorage_prodEnv_endpointIsProdAws() {
    AwsPublicStorage awsPublicStorage =
        new AwsPublicStorage(
            fakeAwsS3Client,
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            instanceOf(Credentials.class),
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.PROD));

    fakeAwsS3Client.addObject("program-summary-image/program-10/myFile10.jpeg");
    awsPublicStorage.prunePublicFileStorage(
        ImmutableSet.of("program-summary-image/program-10/myFile.jpeg"));

    assertThat(fakeAwsS3Client.getLastDeleteEndpointUsed().getHost()).contains("amazonaws");
  }

  @Test
  public void prunePublicFileStorage_devEnv_endpointIsLocalStack() {
    AwsPublicStorage awsPublicStorage =
        new AwsPublicStorage(
            fakeAwsS3Client,
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            instanceOf(Credentials.class),
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.DEV));

    fakeAwsS3Client.addObject("program-summary-image/program-10/myFile10.jpeg");
    awsPublicStorage.prunePublicFileStorage(
        ImmutableSet.of("program-summary-image/program-10/myFile.jpeg"));

    assertThat(fakeAwsS3Client.getLastDeleteEndpointUsed().getHost()).contains("localstack");
  }

  @Test
  public void prunePublicFileStorage_testEnv_endpointIsNotLocalStackOrProdAws() {
    AwsPublicStorage awsPublicStorage =
        new AwsPublicStorage(
            fakeAwsS3Client,
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            instanceOf(Credentials.class),
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.TEST));

    fakeAwsS3Client.addObject("program-summary-image/program-10/myFile10.jpeg");
    awsPublicStorage.prunePublicFileStorage(
        ImmutableSet.of("program-summary-image/program-10/myFile.jpeg"));

    assertThat(fakeAwsS3Client.getLastDeleteEndpointUsed().getHost()).doesNotContain("localstack");
    assertThat(fakeAwsS3Client.getLastDeleteEndpointUsed().getHost()).doesNotContain("amazonaws");
  }

  @Test
  public void prunePublicFileStorage_currentFilesOnlyIncludeInUseFiles_noDeleteRequestIssued() {
    AwsPublicStorage awsPublicStorage =
        new AwsPublicStorage(
            fakeAwsS3Client,
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            instanceOf(Credentials.class),
            instanceOf(Config.class),
            instanceOf(Environment.class));

    fakeAwsS3Client.addObject("program-summary-image/program-10/myFile10.jpeg");
    fakeAwsS3Client.addObject("program-summary-image/program-11/myFile11.jpeg");
    fakeAwsS3Client.addObject("program-summary-image/program-12/myFile12.jpeg");

    awsPublicStorage.prunePublicFileStorage(
        ImmutableSet.of(
            "program-summary-image/program-10/myFile10.jpeg",
            "program-summary-image/program-11/myFile11.jpeg",
            "program-summary-image/program-12/myFile12.jpeg"));

    // Verify there was no delete request issued
    assertThat(fakeAwsS3Client.getLastDeleteEndpointUsed()).isNull();
    // Verify that the list of objects stored still includes all the in-use files
    assertThat(
            fakeAwsS3Client.listObjects(
                instanceOf(Credentials.class),
                Region.US_EAST_2,
                URI.create("fakeEndpoint.com"),
                ListObjectsV2Request.builder().build()))
        .containsExactly(
            "program-summary-image/program-10/myFile10.jpeg",
            "program-summary-image/program-11/myFile11.jpeg",
            "program-summary-image/program-12/myFile12.jpeg");
  }

  @Test
  public void prunePublicFileStorage_currentFilesHasUnused_onlyUnusedDeleted() {
    AwsPublicStorage awsPublicStorage =
        new AwsPublicStorage(
            fakeAwsS3Client,
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            instanceOf(Credentials.class),
            instanceOf(Config.class),
            instanceOf(Environment.class));

    fakeAwsS3Client.addObject("program-summary-image/program-10/myFile10.jpeg");
    fakeAwsS3Client.addObject("program-summary-image/program-11/myFile11.jpeg");
    fakeAwsS3Client.addObject("program-summary-image/program-12/myFile12.jpeg");

    // Only mark the image for program 10 as in-use.
    awsPublicStorage.prunePublicFileStorage(
        ImmutableSet.of("program-summary-image/program-10/myFile10.jpeg"));

    // Verify there a delete request was issued
    assertThat(fakeAwsS3Client.getLastDeleteEndpointUsed()).isNotNull();
    // Verify that the list of objects now only includes the program 10 file
    assertThat(
            fakeAwsS3Client.listObjects(
                instanceOf(Credentials.class),
                Region.US_EAST_2,
                URI.create("fakeEndpoint.com"),
                ListObjectsV2Request.builder().build()))
        .containsExactly("program-summary-image/program-10/myFile10.jpeg");
  }

  @Test
  public void prunePublicFileStorage_inUseFilesHasMissingKey_noError() {
    AwsPublicStorage awsPublicStorage =
        new AwsPublicStorage(
            fakeAwsS3Client,
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            instanceOf(Credentials.class),
            instanceOf(Config.class),
            instanceOf(Environment.class));

    fakeAwsS3Client.addObject("program-summary-image/program-10/myFile10.jpeg");

    // WHEN the in-use file set contains a key that doesn't exist in AWS
    awsPublicStorage.prunePublicFileStorage(
        ImmutableSet.of("program-summary-image/program-11/myFile11.jpeg"));

    // THEN there's no problem and the obsolete program 10 file is deleted
    assertThat(fakeAwsS3Client.getLastDeleteEndpointUsed()).isNotNull();
    assertThat(
            fakeAwsS3Client.listObjects(
                instanceOf(Credentials.class),
                Region.US_EAST_2,
                URI.create("fakeEndpoint.com"),
                ListObjectsV2Request.builder().build()))
        .isEmpty();
  }
}
