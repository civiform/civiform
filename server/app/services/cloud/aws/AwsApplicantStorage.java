package services.cloud.aws;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static services.cloud.aws.AwsStorageUtils.AWS_PRESIGNED_URL_DURATION;

import com.typesafe.config.Config;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.mockito.Mockito;
import play.Environment;
import play.inject.ApplicationLifecycle;
import services.cloud.ApplicantStorageClient;
import services.cloud.StorageServiceName;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/** An AWS Simple Storage Service (S3) implementation of {@link ApplicantStorageClient}. */
@Singleton
public class AwsApplicantStorage implements ApplicantStorageClient {

  private static final String AWS_S3_BUCKET_CONF_PATH = "aws.s3.bucket";
  private static final String AWS_S3_FILE_LIMIT_CONF_PATH = "aws.s3.filelimitmb";

  private final AwsStorageUtils awsStorageUtils;
  private final Region region;
  private final Credentials credentials;
  private final String bucket;
  private final int fileLimitMb;
  private final Client client;

  @Inject
  public AwsApplicantStorage(
      AwsStorageUtils awsStorageUtils,
      AwsRegion region,
      Credentials credentials,
      Config config,
      Environment environment,
      ApplicationLifecycle appLifecycle) {
    this.awsStorageUtils = checkNotNull(awsStorageUtils);
    this.region = checkNotNull(region).get();
    this.credentials = checkNotNull(credentials);
    this.bucket = checkNotNull(config).getString(AWS_S3_BUCKET_CONF_PATH);
    this.fileLimitMb = checkNotNull(config).getInt(AWS_S3_FILE_LIMIT_CONF_PATH);
    if (environment.isDev()) {
      client = new LocalStackClient(config, awsStorageUtils);
    } else if (environment.isTest()) {
      client = new NullClient();
    } else {
      client = new AwsClient(awsStorageUtils);
    }

    appLifecycle.addStopHook(
        () -> {
          client.close();
          return CompletableFuture.completedFuture(null);
        });
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
            .signatureDuration(AWS_PRESIGNED_URL_DURATION)
            .getObjectRequest(getObjectRequest)
            .build();

    PresignedGetObjectRequest presignedGetObjectRequest =
        client.getPresigner().presignGetObject(getObjectPresignRequest);
    return presignedGetObjectRequest.url().toString();
  }

  @Override
  public SignedS3UploadRequest getSignedUploadRequest(
      String fileKey, String successActionRedirect) {
    return awsStorageUtils.getSignedUploadRequest(
        credentials,
        region,
        fileLimitMb,
        bucket,
        client.actionLink(),
        fileKey,
        successActionRedirect);
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

  class AwsClient implements Client {
    private final AwsStorageUtils awsStorageUtils;
    private final S3Presigner presigner;

    AwsClient(AwsStorageUtils awsStorageUtils) {
      this.awsStorageUtils = checkNotNull(awsStorageUtils);
      presigner = S3Presigner.builder().region(region).build();
    }

    @Override
    public S3Presigner getPresigner() {
      return presigner;
    }

    @Override
    public String actionLink() {
      return awsStorageUtils.prodAwsActionLink(bucket, region);
    }

    @Override
    public void close() {
      presigner.close();
    }
  }

  class LocalStackClient implements Client {
    private final Config config;
    private final AwsStorageUtils awsStorageUtils;
    private final S3Presigner presigner;

    LocalStackClient(Config config, AwsStorageUtils awsStorageUtils) {
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
