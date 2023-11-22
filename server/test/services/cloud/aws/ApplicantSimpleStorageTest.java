package services.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.typesafe.config.Config;
import java.io.File;
import org.junit.Test;
import play.Environment;
import play.Mode;
import play.inject.ApplicationLifecycle;
import repository.ResetPostgres;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

public class ApplicantSimpleStorageTest extends ResetPostgres {

  @Test
  public void getSignedUploadRequest_prodEnv_actionLinkIsProdAws() {
    ApplicantSimpleStorage applicantSimpleStorage =
        new ApplicantSimpleStorage(
            instanceOf(AwsRegion.class),
            instanceOf(Credentials.class),
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.PROD),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        applicantSimpleStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.actionLink()).contains("amazonaws.com");
  }

  @Test
  public void getSignedUploadRequest_devEnv_actionLinkIsLocalStack() {
    ApplicantSimpleStorage applicantSimpleStorage =
        new ApplicantSimpleStorage(
            instanceOf(AwsRegion.class),
            instanceOf(Credentials.class),
            instanceOf(Config.class),
            new Environment(new File("."), Environment.class.getClassLoader(), Mode.DEV),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        applicantSimpleStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.actionLink()).contains("localstack");
    assertThat(uploadRequest.actionLink()).doesNotContain("amazonaws.com");
  }

  @Test
  public void getSignedUploadRequest_hasCredentialsAndRegion() {
    AwsRegion region = instanceOf(AwsRegion.class);
    Credentials credentials = instanceOf(Credentials.class);
    ApplicantSimpleStorage applicantSimpleStorage =
        new ApplicantSimpleStorage(
            region,
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        applicantSimpleStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.accessKey()).isEqualTo(credentials.getCredentials().accessKeyId());
    assertThat(uploadRequest.secretKey()).isEqualTo(credentials.getCredentials().secretAccessKey());
    assertThat(uploadRequest.regionName()).isEqualTo(region.get().id());
  }

  @Test
  public void getSignedUploadRequest_hasFileKey() {
    ApplicantSimpleStorage applicantSimpleStorage = instanceOf(ApplicantSimpleStorage.class);

    SignedS3UploadRequest uploadRequest =
        applicantSimpleStorage.getSignedUploadRequest("test/fake/fakeFile.png", "redirect");

    assertThat(uploadRequest.key()).isEqualTo("test/fake/fakeFile.png");
  }

  @Test
  public void getSignedUploadRequest_hasSuccessRedirect() {
    ApplicantSimpleStorage applicantSimpleStorage = instanceOf(ApplicantSimpleStorage.class);

    SignedS3UploadRequest uploadRequest =
        applicantSimpleStorage.getSignedUploadRequest("fileKey", "http://redirect.to.here");

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
    ApplicantSimpleStorage applicantSimpleStorage =
        new ApplicantSimpleStorage(
            instanceOf(AwsRegion.class),
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        applicantSimpleStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.securityToken()).isEqualTo("testSessionToken");
  }

  @Test
  public void getSignedUploadRequest_noSessionCredentials_noSecurityToken() {
    Credentials credentials = mock(Credentials.class);
    AwsCredentials notSessionCredentials = mock(AwsCredentials.class);
    when(notSessionCredentials.accessKeyId()).thenReturn("accessKeyId");
    when(notSessionCredentials.secretAccessKey()).thenReturn("secretKey");
    when(credentials.getCredentials()).thenReturn(notSessionCredentials);

    ApplicantSimpleStorage applicantSimpleStorage =
        new ApplicantSimpleStorage(
            instanceOf(AwsRegion.class),
            credentials,
            instanceOf(Config.class),
            instanceOf(Environment.class),
            instanceOf(ApplicationLifecycle.class));

    SignedS3UploadRequest uploadRequest =
        applicantSimpleStorage.getSignedUploadRequest("fileKey", "redirect");

    assertThat(uploadRequest.securityToken()).isEmpty();
  }
}
