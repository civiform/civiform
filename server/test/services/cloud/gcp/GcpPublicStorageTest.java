package services.cloud.gcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static services.cloud.gcp.GcpPublicStorage.GCP_PUBLIC_S3_BUCKET_CONF_PATH;
import static services.cloud.gcp.GcpPublicStorage.GCP_PUBLIC_S3_FILE_LIMIT_CONF_PATH;
import static support.cloud.FakeS3Client.DELETION_ERROR_FILE_KEY;
import static support.cloud.FakeS3Client.LIST_ERROR_FILE_KEY;

import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import java.io.File;
import java.net.URI;
import org.junit.Test;
import play.Environment;
import play.Mode;
import repository.ResetPostgres;
import services.cloud.aws.Credentials;
import services.cloud.aws.FileListFailureException;
import services.cloud.aws.SignedS3UploadRequest;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import support.cloud.FakeS3Client;

public class GcpPublicStorageTest extends ResetPostgres {
  private static final URI FAKE_URI = URI.create("fakeEndpoint.com");

  private final FakeS3Client fakeS3Client = new FakeS3Client();
  private final Credentials credentials = instanceOf(Credentials.class);

  @Test
  public void getBucketName_returnsBucketFromConfig() {
    GcpPublicStorage gcpPublicStorage = instanceOf(GcpPublicStorage.class);

    assertThat(gcpPublicStorage.getBucketName())
        .isEqualTo(instanceOf(Config.class).getString(GCP_PUBLIC_S3_BUCKET_CONF_PATH));
  }

  @Test
  public void getFileLimitMb_returnsSizeFromConfig() {
    GcpPublicStorage gcpPublicStorage = instanceOf(GcpPublicStorage.class);

    assertThat(gcpPublicStorage.getFileLimitMb())
        .isEqualTo(
            Integer.parseInt(
                instanceOf(Config.class).getString(GCP_PUBLIC_S3_FILE_LIMIT_CONF_PATH)));
  }

  @Test
  public void getSignedUploadRequest_prodEnv_actionLinkIsProdAws() {
    GcpPublicStorage gcpPublicStorage =
        new GcpPublicStorage(
            fakeS3Client,
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            credentials,
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.PROD));

    SignedS3UploadRequest uploadRequest =
        gcpPublicStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.actionLink()).contains("googleapis.com");
    assertThat(uploadRequest.actionLink()).endsWith("/");
  }

  @Test
  public void getSignedUploadRequest_devEnv_actionLinkIsLocalStack() {
    GcpPublicStorage gcpPublicStorage =
        new GcpPublicStorage(
            fakeS3Client,
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            credentials,
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.DEV));

    SignedS3UploadRequest uploadRequest =
        gcpPublicStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.actionLink()).contains("localstack");
    assertThat(uploadRequest.actionLink()).doesNotContain("googleapis.com");
    assertThat(uploadRequest.actionLink()).endsWith("/");
  }

  @Test
  public void getSignedUploadRequest_testEnv_notLocalStackOrProdAws() {
    GcpPublicStorage gcpPublicStorage =
        new GcpPublicStorage(
            fakeS3Client,
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            credentials,
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.TEST));

    SignedS3UploadRequest uploadRequest =
        gcpPublicStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.actionLink()).doesNotContain("localstack");
    assertThat(uploadRequest.actionLink()).doesNotContain("googleapis.com");
  }

  @Test
  public void getSignedUploadRequest_hasCredentialsAndRegion() {
    GcpRegion region = instanceOf(GcpRegion.class);
    GcpPublicStorage gcpPublicStorage =
        new GcpPublicStorage(
            fakeS3Client,
            instanceOf(GcpStorageUtils.class),
            region,
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class));

    SignedS3UploadRequest uploadRequest =
        gcpPublicStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.accessKey()).isEqualTo(credentials.getCredentials().accessKeyId());
    assertThat(uploadRequest.secretKey()).isEqualTo(credentials.getCredentials().secretAccessKey());
    assertThat(uploadRequest.regionName()).isEqualTo(region.get().id());
  }

  @Test
  public void getSignedUploadRequest_hasPublicBucket() {
    GcpPublicStorage gcpPublicStorage = instanceOf(GcpPublicStorage.class);

    SignedS3UploadRequest uploadRequest =
        gcpPublicStorage.getSignedUploadRequest("test/fake/fakeFile.png", "redirect");

    assertThat(uploadRequest.bucket()).isEqualTo("civiform-local-s3-public");
  }

  @Test
  public void getSignedUploadRequest_hasPublicFileLimit() {
    GcpPublicStorage gcpPublicStorage = instanceOf(GcpPublicStorage.class);

    SignedS3UploadRequest uploadRequest =
        gcpPublicStorage.getSignedUploadRequest("test/fake/fakeFile.png", "redirect");

    assertThat(uploadRequest.fileLimitMb()).isEqualTo(1);
  }

  @Test
  public void getSignedUploadRequest_hasFileKey() {
    GcpPublicStorage gcpPublicStorage = instanceOf(GcpPublicStorage.class);

    SignedS3UploadRequest uploadRequest =
        gcpPublicStorage.getSignedUploadRequest("test/fake/fakeFile.png", "redirect");

    assertThat(uploadRequest.key()).isEqualTo("test/fake/fakeFile.png");
  }

  @Test
  public void getSignedUploadRequest_hasSuccessRedirect() {
    GcpPublicStorage gcpPublicStorage = instanceOf(GcpPublicStorage.class);

    SignedS3UploadRequest uploadRequest =
        gcpPublicStorage.getSignedUploadRequest("fileKey", "http://redirect.to.here");

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
    GcpPublicStorage gcpPublicStorage =
        new GcpPublicStorage(
            fakeS3Client,
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class));

    SignedS3UploadRequest uploadRequest =
        gcpPublicStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.securityToken()).isEqualTo("testSessionToken");
  }

  @Test
  public void getSignedUploadRequest_noSessionCredentials_noSecurityToken() {
    Credentials credentials = mock(Credentials.class);
    AwsCredentials notSessionCredentials = mock(AwsCredentials.class);
    when(notSessionCredentials.accessKeyId()).thenReturn("accessKeyId");
    when(notSessionCredentials.secretAccessKey()).thenReturn("secretKey");
    when(credentials.getCredentials()).thenReturn(notSessionCredentials);

    GcpPublicStorage gcpPublicStorage =
        new GcpPublicStorage(
            fakeS3Client,
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class));

    SignedS3UploadRequest uploadRequest =
        gcpPublicStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.securityToken()).isEmpty();
  }

  @Test
  public void getSignedUploadRequest_successActionRedirectTreatedAsExactMatch() {
    GcpPublicStorage gcpPublicStorage = instanceOf(GcpPublicStorage.class);

    SignedS3UploadRequest uploadRequest =
        gcpPublicStorage.getSignedUploadRequest("test/fake/fakeFile.png", "redirect");

    assertThat(uploadRequest.useSuccessActionRedirectAsPrefix()).isFalse();
  }

  @Test
  public void getPublicDisplayUrl_incorrectlyFormatted_throws() {
    GcpPublicStorage gcpPublicStorage = instanceOf(GcpPublicStorage.class);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> gcpPublicStorage.getPublicDisplayUrl("fake-file-key"))
        .withMessageContaining("key incorrectly formatted");
  }

  @Test
  public void getPublicDisplayUrl_correctlyFormatted_hasActionLinkAndFileKey() {
    GcpPublicStorage gcpPublicStorage = instanceOf(GcpPublicStorage.class);

    String publicDisplayUrl =
        gcpPublicStorage.getPublicDisplayUrl("program-summary-image/program-10/myFile.jpeg");

    assertThat(publicDisplayUrl)
        .isEqualTo("fake-action-link/program-summary-image/program-10/myFile.jpeg");
  }

  @Test
  public void prunePublicFileStorage_prodEnv_endpointIsProdAws() {
    GcpPublicStorage gcpPublicStorage =
        new GcpPublicStorage(
            fakeS3Client,
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            credentials,
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.PROD));

    // Add an object so that pruning actually needs to delete something.
    fakeS3Client.addObject("program-summary-image/program-10/myFile10.jpeg");
    gcpPublicStorage.prunePublicFileStorage(ImmutableSet.of());

    assertThat(fakeS3Client.getLastDeleteEndpointUsed().getHost()).contains("googleapis");
  }

  @Test
  public void prunePublicFileStorage_devEnv_endpointIsLocalStack() {
    GcpPublicStorage gcpPublicStorage =
        new GcpPublicStorage(
            fakeS3Client,
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            credentials,
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.DEV));

    // Add an object so that pruning actually needs to delete something.
    fakeS3Client.addObject("program-summary-image/program-10/myFile10.jpeg");
    gcpPublicStorage.prunePublicFileStorage(ImmutableSet.of());

    assertThat(fakeS3Client.getLastDeleteEndpointUsed().getHost()).contains("localstack");
  }

  @Test
  public void prunePublicFileStorage_testEnv_endpointIsNotLocalStackOrProdAws() {
    GcpPublicStorage gcpPublicStorage =
        new GcpPublicStorage(
            fakeS3Client,
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            credentials,
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.TEST));

    // Add an object so that pruning actually needs to delete something.
    fakeS3Client.addObject("program-summary-image/program-10/myFile10.jpeg");
    gcpPublicStorage.prunePublicFileStorage(ImmutableSet.of());

    assertThat(fakeS3Client.getLastDeleteEndpointUsed().getHost()).doesNotContain("localstack");
    assertThat(fakeS3Client.getLastDeleteEndpointUsed().getHost()).doesNotContain("googleapis");
  }

  @Test
  public void prunePublicFileStorage_currentFilesOnlyIncludeInUseFiles_noDeleteRequestIssued()
      throws FileListFailureException {
    GcpPublicStorage gcpPublicStorage =
        new GcpPublicStorage(
            fakeS3Client,
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class));

    fakeS3Client.addObject("program-summary-image/program-10/myFile10.jpeg");
    fakeS3Client.addObject("program-summary-image/program-11/myFile11.jpeg");
    fakeS3Client.addObject("program-summary-image/program-12/myFile12.jpeg");

    gcpPublicStorage.prunePublicFileStorage(
        ImmutableSet.of(
            "program-summary-image/program-10/myFile10.jpeg",
            "program-summary-image/program-11/myFile11.jpeg",
            "program-summary-image/program-12/myFile12.jpeg"));

    // Verify there was no delete request issued
    assertThat(fakeS3Client.getLastDeleteEndpointUsed()).isNull();
    // Verify that the list of objects stored still includes all the in-use files
    assertThat(
            fakeS3Client.listObjects(
                credentials, Region.US_EAST_2, FAKE_URI, ListObjectsV2Request.builder().build()))
        .containsExactly(
            "program-summary-image/program-10/myFile10.jpeg",
            "program-summary-image/program-11/myFile11.jpeg",
            "program-summary-image/program-12/myFile12.jpeg");
  }

  @Test
  public void prunePublicFileStorage_someUnusedFiles_onlyUnusedDeleted()
      throws FileListFailureException {
    GcpPublicStorage gcpPublicStorage =
        new GcpPublicStorage(
            fakeS3Client,
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class));

    fakeS3Client.addObject("program-summary-image/program-10/myFile10.jpeg");
    fakeS3Client.addObject("program-summary-image/program-11/myFile11.jpeg");
    fakeS3Client.addObject("program-summary-image/program-12/myFile12.jpeg");

    // Only mark the image for program 10 as in-use.
    gcpPublicStorage.prunePublicFileStorage(
        ImmutableSet.of("program-summary-image/program-10/myFile10.jpeg"));

    // Verify a delete request was issued
    assertThat(fakeS3Client.getLastDeleteEndpointUsed()).isNotNull();
    // Verify that the list of objects now only includes the program 10 file
    assertThat(
            fakeS3Client.listObjects(
                credentials, Region.US_EAST_2, FAKE_URI, ListObjectsV2Request.builder().build()))
        .containsExactly("program-summary-image/program-10/myFile10.jpeg");
  }

  @Test
  public void prunePublicFileStorage_inUseFilesHasMissingKey_noError()
      throws FileListFailureException {
    GcpPublicStorage gcpPublicStorage =
        new GcpPublicStorage(
            fakeS3Client,
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class));

    fakeS3Client.addObject("program-summary-image/program-10/myFile10.jpeg");

    // WHEN the in-use file set contains a key that doesn't exist in AWS
    gcpPublicStorage.prunePublicFileStorage(
        ImmutableSet.of("program-summary-image/program-11/myFile11.jpeg"));

    // THEN there's no problem and the unused program 10 file is still deleted
    assertThat(fakeS3Client.getLastDeleteEndpointUsed()).isNotNull();
    assertThat(
            fakeS3Client.listObjects(
                credentials, Region.US_EAST_2, FAKE_URI, ListObjectsV2Request.builder().build()))
        .isEmpty();
  }

  @Test
  public void prunePublicFileStorage_fileListFailure_nothingDeleted() {
    GcpPublicStorage gcpPublicStorage =
        new GcpPublicStorage(
            fakeS3Client,
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class));

    fakeS3Client.addObject("program-summary-image/program-11/myFile11.jpeg");
    // WHEN AWS has a key that's hard-coded to throw the FileListFailureException when listing
    // objects
    fakeS3Client.addObject(LIST_ERROR_FILE_KEY);

    gcpPublicStorage.prunePublicFileStorage(ImmutableSet.of());

    // THEN no deletion was attempted
    assertThat(fakeS3Client.getLastDeleteEndpointUsed()).isNull();
  }

  @Test
  public void prunePublicFileStorage_fileDeletionFailure_notThrown() {
    GcpPublicStorage gcpPublicStorage =
        new GcpPublicStorage(
            fakeS3Client,
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class));

    // WHEN the in-use file set contains a key that's hard-coded to throw the
    // FileDeletionFailureException
    gcpPublicStorage.prunePublicFileStorage(ImmutableSet.of(DELETION_ERROR_FILE_KEY));

    // THEN we want that exception is handled internally in GcpPublicStorage and not re-thrown. This
    // test doesn't need an assert, it just verifies there was no exception.
  }
}
