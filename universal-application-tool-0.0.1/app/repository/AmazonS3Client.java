package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import play.inject.ApplicationLifecycle;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Singleton
public class AmazonS3Client {
  public static final String AWS_ENABLE = "aws.enable";
  public static final String AWS_LOCAL_ENDPOINT = "aws.local.endpoint";
  public static final String AWS_S3_REGION = "aws.s3.region";
  public static final String AWS_S3_BUCKET = "aws.s3.bucket";
  public static final Duration AWS_PRESIGNED_URL_DURATION = Duration.ofMinutes(10);
  private static final Logger log = LoggerFactory.getLogger("s3client");

  private final ApplicationLifecycle appLifecycle;
  private final Config config;
  private final Environment environment;
  private final AwsCredentials credentials;
  private Region region;
  private String bucket;
  private S3Presigner presigner;

  @Inject
  public AmazonS3Client(ApplicationLifecycle appLifecycle, Config config, Environment environment) {
    this.appLifecycle = checkNotNull(appLifecycle);
    this.config = checkNotNull(config);
    this.environment = checkNotNull(environment);

    log.info("aws s3 enabled: " + String.valueOf(enabled()));
    if (!enabled()) {
      this.credentials = null;
      return;
    }
    this.credentials = DefaultCredentialsProvider.create().resolveCredentials();
    connect();

    this.appLifecycle.addStopHook(
        () -> {
          if (presigner != null) {
            presigner.close();
          }
          return CompletableFuture.completedFuture(null);
        });
  }

  public boolean enabled() {
    if (!config.hasPath(AWS_ENABLE) || !config.getBoolean(AWS_ENABLE)) {
      return false;
    }
    return (config.hasPath(AWS_S3_REGION) && config.hasPath(AWS_S3_BUCKET));
  }

  public URL getPresignedUrl(String key) {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder().key(key).bucket(bucket).build();

    GetObjectPresignRequest getObjectPresignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(AWS_PRESIGNED_URL_DURATION)
            .getObjectRequest(getObjectRequest)
            .build();

    PresignedGetObjectRequest presignedGetObjectRequest =
        presigner.presignGetObject(getObjectPresignRequest);
    return presignedGetObjectRequest.url();
  }

  public SignedS3UploadRequest getSignedUploadRequest(String key, String successActionRedirect) {
    SignedS3UploadRequest.Builder builder =
        SignedS3UploadRequest.builder()
            .setKey(key)
            .setSuccessActionRedirect(successActionRedirect)
            .setAccessKey(credentials.accessKeyId())
            .setExpirationDuration(AWS_PRESIGNED_URL_DURATION)
            .setBucket(bucket)
            .setSecretKey(credentials.secretAccessKey())
            .setRegionName(region.id());
    if (credentials instanceof AwsSessionCredentials) {
      AwsSessionCredentials sessionCredentials = (AwsSessionCredentials) credentials;
      builder.setSecurityToken(sessionCredentials.sessionToken());
    }
    return builder.build();
  }

  private void connect() {
    String regionName = config.getString(AWS_S3_REGION);
    region = Region.of(regionName);
    bucket = config.getString(AWS_S3_BUCKET);

    S3Presigner.Builder s3PresignerBuilder = S3Presigner.builder().region(region);
    if (environment.isDev()) {
      try {
        URI localUri = new URI(config.getString(AWS_LOCAL_ENDPOINT));
        s3PresignerBuilder = s3PresignerBuilder.endpointOverride(localUri);
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
    presigner = s3PresignerBuilder.build();
  }
}
