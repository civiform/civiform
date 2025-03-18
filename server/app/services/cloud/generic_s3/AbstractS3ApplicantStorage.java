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
import services.cloud.aws.Credentials;
import services.cloud.aws.SignedS3UploadRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/** An Simple Storage Service (S3) implementation of {@link ApplicantStorageClient}. */
public abstract class AbstractS3ApplicantStorage implements ApplicantStorageClient {
  private final AbstractS3StorageUtils s3StorageUtils;
  private final Region region;
  private final Credentials credentials;
  private final String bucket;
  private final int fileLimitMb;
  private final AbstractS3ApplicantStorage.Client client;

  public AbstractS3ApplicantStorage(
      AbstractS3StorageUtils s3StorageUtils,
      AbstractS3Region region,
      Credentials credentials,
      Config config,
      Environment environment,
      ApplicationLifecycle appLifecycle) {
    this.s3StorageUtils = checkNotNull(s3StorageUtils);
    this.region = checkNotNull(region).get();
    this.credentials = checkNotNull(credentials);
    this.bucket = checkNotNull(config).getString(getBucketConfigPath());
    this.fileLimitMb = checkNotNull(config).getInt(getFileLimitMbPath());
    if (environment.isDev()) {
      client = new AbstractS3ApplicantStorage.LocalStackClient(config, s3StorageUtils);
    } else if (environment.isTest()) {
      client = new AbstractS3ApplicantStorage.NullClient();
    } else {
      client = new S3Client(s3StorageUtils);
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
    return s3StorageUtils.getSignedUploadRequest(
        credentials,
        region,
        fileLimitMb,
        bucket,
        client.actionLink(),
        fileKey,
        successActionRedirectPrefix,
        /* useSuccessActionRedirectAsPrefix= */ true);
  }

  interface Client {

    S3Presigner getPresigner();

    /** Returns the action link that applicant files should be sent to. Must end in a `/`. */
    String actionLink();

    void close();
  }

  static class NullClient implements Client {

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

  class S3Client implements Client {
    private final AbstractS3StorageUtils s3StorageUtils;
    private final S3Presigner presigner;

    S3Client(AbstractS3StorageUtils s3StorageUtils) {
      this.s3StorageUtils = checkNotNull(s3StorageUtils);
      presigner = S3Presigner.builder().region(region).build();
    }

    @Override
    public S3Presigner getPresigner() {
      return presigner;
    }

    @Override
    public String actionLink() {
      return s3StorageUtils.prodActionLink(bucket, region);
    }

    @Override
    public void close() {
      presigner.close();
    }
  }

  class LocalStackClient implements Client {
    private final Config config;
    private final AbstractS3StorageUtils s3StorageUtils;
    private final S3Presigner presigner;

    LocalStackClient(Config config, AbstractS3StorageUtils s3StorageUtils) {
      this.config = checkNotNull(config);
      this.s3StorageUtils = checkNotNull(s3StorageUtils);
      this.presigner =
          S3Presigner.builder()
              .endpointOverride(s3StorageUtils.localStackEndpoint(config))
              .region(region)
              .build();
    }

    @Override
    public S3Presigner getPresigner() {
      return presigner;
    }

    @Override
    public String actionLink() {
      return s3StorageUtils.localStackActionLink(config, bucket, region);
    }

    @Override
    public void close() {
      presigner.close();
    }
  }
}
