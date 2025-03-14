package services.cloud.generic_s3;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static services.cloud.aws.AwsStorageUtils.PRESIGNED_URL_DURATION;

import com.typesafe.config.Config;
import controllers.applicant.ApplicantRequestedAction;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.mockito.Mockito;
import play.Environment;
import play.inject.ApplicationLifecycle;
import services.cloud.ApplicantStorageClient;
import services.cloud.StorageServiceName;
import services.cloud.aws.Credentials;
import services.cloud.aws.SignedS3UploadRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/** An Simple Storage Service (S3) implementation of {@link ApplicantStorageClient}. */
public abstract class AbstractS3ApplicantStorage implements ApplicantStorageClient {
  private final AbstractS3StorageUtils awsStorageUtils;
  private final Region region;
  private final Credentials credentials;
  private final String bucket;
  private final int fileLimitMb;
  private final AbstractS3ApplicantStorage.Client client;

  public AbstractS3ApplicantStorage(
      AbstractS3StorageUtils awsStorageUtils,
      AbstractS3Region region,
      Credentials credentials,
      Config config,
      Environment environment,
      ApplicationLifecycle appLifecycle) {
    this.awsStorageUtils = checkNotNull(awsStorageUtils);
    this.region = checkNotNull(region).get();
    this.credentials = checkNotNull(credentials);
    this.bucket = checkNotNull(config).getString(getBucketConfigPath());
    this.fileLimitMb = checkNotNull(config).getInt(getFileLimitMbPath());
    if (environment.isDev()) {
      client = new AbstractS3ApplicantStorage.LocalStackClient(config, awsStorageUtils);
    } else if (environment.isTest()) {
      client = new AbstractS3ApplicantStorage.NullClient();
    } else {
      client = new AbstractS3ApplicantStorage.AwsClient(awsStorageUtils);
    }

    appLifecycle.addStopHook(
        () -> {
          client.close();
          return CompletableFuture.completedFuture(null);
        });
  }

  /** The bucket path defined in the conf file */
  protected abstract String getBucketConfigPath();

  /** The filelimitmb path in the conf file */
  protected abstract String getFileLimitMbPath();

  @Override
  public int getFileLimitMb() {
    return fileLimitMb;
  }

  @Override
  public String getPresignedUrlString(String fileKey) {
    // TODO(#1841): support storing and displaying original filenames for AWS uploads
    return getPresignedUrlString(fileKey, /* originalFileName= */ Optional.empty());
  }

  @Override
  public String getPresignedUrlString(String fileKey, Optional<String> originalFileName) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().key(fileKey).bucket(bucket).build();
    GetObjectPresignRequest getObjectPresignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(PRESIGNED_URL_DURATION)
            .getObjectRequest(getObjectRequest)
            .build();

    PresignedGetObjectRequest presignedGetObjectRequest =
        client.getPresigner().presignGetObject(getObjectPresignRequest);
    return presignedGetObjectRequest.url().toString();
  }

  @Override
  public SignedS3UploadRequest getSignedUploadRequest(
      String fileKey, String successActionRedirectUrl) {
    // For the file upload question, assets/javascripts/file_upload.ts may modify the
    // applicant-requested action part of the success_action_redirect URL to specify where the
    // user should be taken after the file has been successfully uploaded. So, the redirect
    // URL we send to {@link SignedS3UploadRequest} needs to have that action removed and needs
    // the redirect URL to be considered just a prefix so that the applicant-requested action at
    // the end of the URL can be changed without causing an AWS policy error. See {@link
    // SignedS3UploadRequest#useSuccessActionRedirectAsPrefix} for more details.
    String successActionRedirectPrefix =
        ApplicantRequestedAction.stripActionFromEndOfUrl(successActionRedirectUrl);
    return awsStorageUtils.getSignedUploadRequest(
        credentials,
        region,
        fileLimitMb,
        bucket,
        client.actionLink(),
        fileKey,
        successActionRedirectPrefix,
        /* useSuccessActionRedirectAsPrefix= */ true);
  }

  @Override
  public StorageServiceName getStorageServiceName() {
    return StorageServiceName.AWS_S3;
  }

  interface Client {

    S3Presigner getPresigner();

    /** Returns the action link that applicant files should be sent to. Must end in a `/`. */
    String actionLink();

    void close();
  }

  static class NullClient implements AbstractS3ApplicantStorage.Client {

    private final S3Presigner presigner;

    NullClient() {
      presigner = Mockito.mock(S3Presigner.class);
      PresignedGetObjectRequest presignedGetObjectRequest =
          Mockito.mock(PresignedGetObjectRequest.class);
      URL fakeUrl;
      try {
        fakeUrl = new URL("http://fake-url");
      } catch (java.net.MalformedURLException e) {
        throw new RuntimeException(e);
      }
      when(presignedGetObjectRequest.url()).thenReturn(fakeUrl);
      when(presigner.presignGetObject(any(GetObjectPresignRequest.class)))
          .thenReturn(presignedGetObjectRequest);
    }

    @Override
    public S3Presigner getPresigner() {
      return presigner;
    }

    @Override
    public String actionLink() {
      return "fake-action-link/";
    }

    @Override
    public void close() {}
  }

  class AwsClient implements AbstractS3ApplicantStorage.Client {
    private final AbstractS3StorageUtils awsStorageUtils;
    private final S3Presigner presigner;

    AwsClient(AbstractS3StorageUtils awsStorageUtils) {
      this.awsStorageUtils = checkNotNull(awsStorageUtils);
      presigner = S3Presigner.builder().region(region).build();
    }

    @Override
    public S3Presigner getPresigner() {
      return presigner;
    }

    @Override
    public String actionLink() {
      return awsStorageUtils.prodActionLink(bucket, region);
    }

    @Override
    public void close() {
      presigner.close();
    }
  }

  class LocalStackClient implements AbstractS3ApplicantStorage.Client {
    private final Config config;
    private final AbstractS3StorageUtils awsStorageUtils;
    private final S3Presigner presigner;

    LocalStackClient(Config config, AbstractS3StorageUtils awsStorageUtils) {
      this.config = checkNotNull(config);
      this.awsStorageUtils = checkNotNull(awsStorageUtils);
      this.presigner =
          S3Presigner.builder()
              .endpointOverride(awsStorageUtils.localStackEndpoint(config))
              .region(region)
              .build();
    }

    @Override
    public S3Presigner getPresigner() {
      return presigner;
    }

    @Override
    public String actionLink() {
      return awsStorageUtils.localStackActionLink(config, bucket, region);
    }

    @Override
    public void close() {
      presigner.close();
    }
  }
}
