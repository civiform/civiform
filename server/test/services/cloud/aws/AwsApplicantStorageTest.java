package services.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;
import static services.cloud.aws.AwsApplicantStorage.AWS_S3_FILE_LIMIT_CONF_PATH;

import com.typesafe.config.Config;
import java.io.File;
import org.junit.Test;
import play.Environment;
import play.Mode;
import play.inject.ApplicationLifecycle;
import repository.ResetPostgres;
import services.settings.SettingsManifest;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

public class AwsApplicantStorageTest extends ResetPostgres {
  @Test
  public void getFileLimitMb_returnsSizeFromConfig() {
    AwsApplicantStorage awsApplicantStorage = instanceOf(AwsApplicantStorage.class);

    assertThat(awsApplicantStorage.getFileLimitMb())
        .isEqualTo(
            Integer.parseInt(instanceOf(Config.class).getString(AWS_S3_FILE_LIMIT_CONF_PATH)));
  }

  @Test
  public void getSignedUploadRequest_prodEnv_actionLinkIsProdAws() {
    AwsApplicantStorage awsApplicantStorage =
        new AwsApplicantStorage(
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            instanceOf(Credentials.class),
            instanceOf(SettingsManifest.class),
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.PROD),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        awsApplicantStorage.getSignedUploadRequest("fileKey", "redirect", fakeRequest().build());

    assertThat(uploadRequest.actionLink()).contains("amazonaws.com");
    assertThat(uploadRequest.actionLink()).endsWith("/");
  }

  @Test
  public void getSignedUploadRequest_devEnv_actionLinkIsLocalStack() {
    AwsApplicantStorage awsApplicantStorage =
        new AwsApplicantStorage(
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            instanceOf(Credentials.class),
            instanceOf(SettingsManifest.class),
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.DEV),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        awsApplicantStorage.getSignedUploadRequest("fileKey", "redirect", fakeRequest().build());

    assertThat(uploadRequest.actionLink()).contains("localstack");
    assertThat(uploadRequest.actionLink()).doesNotContain("amazonaws.com");
    assertThat(uploadRequest.actionLink()).endsWith("/");
  }

  @Test
  public void getSignedUploadRequest_hasCredentialsAndRegion() {
    AwsRegion region = instanceOf(AwsRegion.class);
    Credentials credentials = instanceOf(Credentials.class);
    AwsApplicantStorage awsApplicantStorage =
        new AwsApplicantStorage(
            instanceOf(AwsStorageUtils.class),
            region,
            credentials,
            instanceOf(SettingsManifest.class),
            instanceOf(Config.class),
            instanceOf(Environment.class),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        awsApplicantStorage.getSignedUploadRequest("fileKey", "redirect", fakeRequest().build());

    assertThat(uploadRequest.accessKey()).isEqualTo(credentials.getCredentials().accessKeyId());
    assertThat(uploadRequest.secretKey()).isEqualTo(credentials.getCredentials().secretAccessKey());
    assertThat(uploadRequest.regionName()).isEqualTo(region.get().id());
  }

  @Test
  public void getSignedUploadRequest_hasApplicantBucket() {
    AwsApplicantStorage awsApplicantStorage = instanceOf(AwsApplicantStorage.class);

    SignedS3UploadRequest uploadRequest =
        awsApplicantStorage.getSignedUploadRequest(
            "test/fake/fakeFile.png", "redirect", fakeRequest().build());

    assertThat(uploadRequest.bucket()).isEqualTo("civiform-local-s3");
  }

  @Test
  public void getSignedUploadRequest_hasApplicantFileLimit() {
    AwsApplicantStorage awsApplicantStorage = instanceOf(AwsApplicantStorage.class);

    SignedS3UploadRequest uploadRequest =
        awsApplicantStorage.getSignedUploadRequest(
            "test/fake/fakeFile.png", "redirect", fakeRequest().build());

    assertThat(uploadRequest.fileLimitMb()).isEqualTo(100);
  }

  @Test
  public void getSignedUploadRequest_hasFileKey() {
    AwsApplicantStorage awsApplicantStorage = instanceOf(AwsApplicantStorage.class);

    SignedS3UploadRequest uploadRequest =
        awsApplicantStorage.getSignedUploadRequest(
            "test/fake/fakeFile.png", "redirect", fakeRequest().build());

    assertThat(uploadRequest.key()).isEqualTo("test/fake/fakeFile.png");
  }

  @Test
  public void getSignedUploadRequest_hasSuccessRedirect() {
    AwsApplicantStorage awsApplicantStorage = instanceOf(AwsApplicantStorage.class);

    SignedS3UploadRequest uploadRequest =
        awsApplicantStorage.getSignedUploadRequest(
            "fileKey", "http://redirect.to.here", fakeRequest().build());

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
    AwsApplicantStorage awsApplicantStorage =
        new AwsApplicantStorage(
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            credentials,
            instanceOf(SettingsManifest.class),
            instanceOf(Config.class),
            instanceOf(Environment.class),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        awsApplicantStorage.getSignedUploadRequest("fileKey", "redirect", fakeRequest().build());

    assertThat(uploadRequest.securityToken()).isEqualTo("testSessionToken");
  }

  @Test
  public void getSignedUploadRequest_noSessionCredentials_noSecurityToken() {
    Credentials credentials = mock(Credentials.class);
    AwsCredentials notSessionCredentials = mock(AwsCredentials.class);
    when(notSessionCredentials.accessKeyId()).thenReturn("accessKeyId");
    when(notSessionCredentials.secretAccessKey()).thenReturn("secretKey");
    when(credentials.getCredentials()).thenReturn(notSessionCredentials);

    AwsApplicantStorage awsApplicantStorage =
        new AwsApplicantStorage(
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            credentials,
            instanceOf(SettingsManifest.class),
            instanceOf(Config.class),
            instanceOf(Environment.class),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        awsApplicantStorage.getSignedUploadRequest("fileKey", "redirect", fakeRequest().build());

    assertThat(uploadRequest.securityToken()).isEmpty();
  }

  /** Regression test for https://github.com/civiform/civiform/issues/6737. */
  @Test
  public void getSignedUploadRequest_saveOnAllActionsFlagOn_successActionRedirectTreatedAsPrefix() {
    SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
    AwsApplicantStorage awsApplicantStorage =
        new AwsApplicantStorage(
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            instanceOf(Credentials.class),
            mockSettingsManifest,
            instanceOf(Config.class),
            instanceOf(Environment.class),
            instanceOf(ApplicationLifecycle.class));

    when(mockSettingsManifest.getSaveOnAllActions(any())).thenReturn(true);

    SignedS3UploadRequest uploadRequest =
        awsApplicantStorage.getSignedUploadRequest(
            "fileKey",
            "https://civiform.dev/programs/4/blocks/1/updateFile/true/NEXT_BLOCK",
            fakeRequest().build());

    // Verify the applicant-requested action at the end of the redirect URL was removed, so that the
    // redirect URL is valid for all types of applicant-requested actions.
    assertThat(uploadRequest.successActionRedirect())
        .isEqualTo("https://civiform.dev/programs/4/blocks/1/updateFile/true");
    // Verify that the redirect URL is treated as a prefix.
    assertThat(uploadRequest.useSuccessActionRedirectAsPrefix()).isTrue();
  }

  @Test
  public void
      getSignedUploadRequest_saveOnAllActionsFlagOff_successActionRedirectTreatedAsExactMatch() {
    SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
    AwsApplicantStorage awsApplicantStorage =
        new AwsApplicantStorage(
            instanceOf(AwsStorageUtils.class),
            instanceOf(AwsRegion.class),
            instanceOf(Credentials.class),
            mockSettingsManifest,
            instanceOf(Config.class),
            instanceOf(Environment.class),
            instanceOf(ApplicationLifecycle.class));

    when(mockSettingsManifest.getSaveOnAllActions(any())).thenReturn(false);

    SignedS3UploadRequest uploadRequest =
        awsApplicantStorage.getSignedUploadRequest(
            "fileKey",
            "https://civiform.dev/programs/4/blocks/1/updateFile/true/NEXT_BLOCK",
            fakeRequest().build());

    // Verify the redirect URL wasn't modified
    assertThat(uploadRequest.successActionRedirect())
        .isEqualTo("https://civiform.dev/programs/4/blocks/1/updateFile/true/NEXT_BLOCK");
    // Verify that the redirect URL is not being treated as a prefix.
    assertThat(uploadRequest.useSuccessActionRedirectAsPrefix()).isFalse();
  }
}
