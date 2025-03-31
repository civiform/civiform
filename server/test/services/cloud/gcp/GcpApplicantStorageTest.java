package services.cloud.gcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static services.cloud.gcp.GcpApplicantStorage.GCP_S3_FILE_LIMIT_CONF_PATH;

import com.typesafe.config.Config;
import java.io.File;
import org.junit.Test;
import play.Environment;
import play.Mode;
import play.inject.ApplicationLifecycle;
import repository.ResetPostgres;
import services.cloud.aws.Credentials;
import services.cloud.aws.SignedS3UploadRequest;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

public class GcpApplicantStorageTest extends ResetPostgres {
  @Test
  public void getFileLimitMb_returnsSizeFromConfig() {
    GcpApplicantStorage gcpApplicantStorage = instanceOf(GcpApplicantStorage.class);

    assertThat(gcpApplicantStorage.getFileLimitMb())
        .isEqualTo(
            Integer.parseInt(instanceOf(Config.class).getString(GCP_S3_FILE_LIMIT_CONF_PATH)));
  }

  @Test
  public void getSignedUploadRequest_prodEnv_actionLinkIsProd() {
    GcpApplicantStorage gcpApplicantStorage =
        new GcpApplicantStorage(
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            instanceOf(Credentials.class),
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.PROD),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        gcpApplicantStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.actionLink()).contains("googleapis.com");
    assertThat(uploadRequest.actionLink()).endsWith("/");
  }

  @Test
  public void getSignedUploadRequest_devEnv_actionLinkIsLocalStack() {
    GcpApplicantStorage gcpApplicantStorage =
        new GcpApplicantStorage(
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            instanceOf(Credentials.class),
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.DEV),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        gcpApplicantStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.actionLink()).contains("localstack");
    assertThat(uploadRequest.actionLink()).doesNotContain("googleapis.com");
    assertThat(uploadRequest.actionLink()).endsWith("/");
  }

  @Test
  public void getSignedUploadRequest_hasCredentialsAndRegion() {
    GcpRegion region = instanceOf(GcpRegion.class);
    Credentials credentials = instanceOf(Credentials.class);
    GcpApplicantStorage gcpApplicantStorage =
        new GcpApplicantStorage(
            instanceOf(GcpStorageUtils.class),
            region,
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        gcpApplicantStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.accessKey()).isEqualTo(credentials.getCredentials().accessKeyId());
    assertThat(uploadRequest.secretKey()).isEqualTo(credentials.getCredentials().secretAccessKey());
    assertThat(uploadRequest.regionName()).isEqualTo(region.get().id());
  }

  @Test
  public void getSignedUploadRequest_hasApplicantBucket() {
    GcpApplicantStorage gcpApplicantStorage = instanceOf(GcpApplicantStorage.class);

    SignedS3UploadRequest uploadRequest =
        gcpApplicantStorage.getSignedUploadRequest("test/fake/fakeFile.png", "redirect");

    assertThat(uploadRequest.bucket()).isEqualTo("civiform-local-s3");
  }

  @Test
  public void getSignedUploadRequest_hasApplicantFileLimit() {
    GcpApplicantStorage gcpApplicantStorage = instanceOf(GcpApplicantStorage.class);

    SignedS3UploadRequest uploadRequest =
        gcpApplicantStorage.getSignedUploadRequest("test/fake/fakeFile.png", "redirect");

    assertThat(uploadRequest.fileLimitMb()).isEqualTo(100);
  }

  @Test
  public void getSignedUploadRequest_hasFileKey() {
    GcpApplicantStorage gcpApplicantStorage = instanceOf(GcpApplicantStorage.class);

    SignedS3UploadRequest uploadRequest =
        gcpApplicantStorage.getSignedUploadRequest("test/fake/fakeFile.png", "redirect");

    assertThat(uploadRequest.key()).isEqualTo("test/fake/fakeFile.png");
  }

  @Test
  public void getSignedUploadRequest_hasSuccessRedirect() {
    GcpApplicantStorage gcpApplicantStorage = instanceOf(GcpApplicantStorage.class);

    SignedS3UploadRequest uploadRequest =
        gcpApplicantStorage.getSignedUploadRequest("fileKey", "http://redirect.to.here");

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
    GcpApplicantStorage gcpApplicantStorage =
        new GcpApplicantStorage(
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        gcpApplicantStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.securityToken()).isEqualTo("testSessionToken");
  }

  @Test
  public void getSignedUploadRequest_noSessionCredentials_noSecurityToken() {
    Credentials credentials = mock(Credentials.class);
    AwsCredentials notSessionCredentials = mock(AwsCredentials.class);
    when(notSessionCredentials.accessKeyId()).thenReturn("accessKeyId");
    when(notSessionCredentials.secretAccessKey()).thenReturn("secretKey");
    when(credentials.getCredentials()).thenReturn(notSessionCredentials);

    GcpApplicantStorage gcpApplicantStorage =
        new GcpApplicantStorage(
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        gcpApplicantStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.securityToken()).isEmpty();
  }

  /** Regression test for https://github.com/civiform/civiform/issues/6737. */
  @Test
  public void getSignedUploadRequest_successActionRedirectTreatedAsPrefix() {
    GcpApplicantStorage gcpApplicantStorage =
        new GcpApplicantStorage(
            instanceOf(GcpStorageUtils.class),
            instanceOf(GcpRegion.class),
            instanceOf(Credentials.class),
            instanceOf(Config.class),
            instanceOf(Environment.class),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        gcpApplicantStorage.getSignedUploadRequest(
            "fileKey", "https://civiform.dev/programs/4/blocks/1/updateFile/true/NEXT_BLOCK");

    // Verify the applicant-requested action at the end of the redirect URL was removed, so that the
    // redirect URL is valid for all types of applicant-requested actions.
    assertThat(uploadRequest.successActionRedirect())
        .isEqualTo("https://civiform.dev/programs/4/blocks/1/updateFile/true");
    // Verify that the redirect URL is treated as a prefix.
    assertThat(uploadRequest.useSuccessActionRedirectAsPrefix()).isTrue();
  }
}
