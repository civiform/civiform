package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import play.inject.ApplicationLifecycle;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Singleton
public class AmazonS3Client {
  public static final String AWS_S3_REGION = "aws.s3.region";
  public static final String AWS_S3_BUCKET = "aws.s3.bucket";
  public static final String AWS_LOCAL_ENDPOINT = "aws.local.endpoint";
  private static final Logger log = LoggerFactory.getLogger("s3client");

  private final ApplicationLifecycle appLifecycle;
  private final Config config;
  private final Environment environment;
  private Region region;
  private String bucket;
  private S3Client s3;

  @Inject
  public AmazonS3Client(ApplicationLifecycle appLifecycle, Config config, Environment environment) {
    this.appLifecycle = checkNotNull(appLifecycle);
    this.config = checkNotNull(config);
    this.environment = checkNotNull(environment);

    log.info("aws s3 enabled: " + String.valueOf(enabled()));
    if (enabled()) {
      connect();
      putTestObject();
      getTestObject();
    }

    this.appLifecycle.addStopHook(
        () -> {
          if (s3 != null) {
            s3.close();
          }
          return CompletableFuture.completedFuture(null);
        });
  }

  public boolean enabled() {
    return (config.hasPath(AWS_S3_REGION) && config.hasPath(AWS_S3_BUCKET));
  }

  public void putObject(String key, byte[] data) {
    ensureS3Client();

    try {
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder().bucket(bucket).key(key).build();
      s3.putObject(putObjectRequest, RequestBody.fromBytes(data));
    } catch (S3Exception e) {
      throw new RuntimeException("S3 exception: " + e.getMessage());
    }
  }

  public byte[] getObject(String key) {
    ensureS3Client();

    try {
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().key(key).bucket(bucket).build();
      ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(getObjectRequest);
      return objectBytes.asByteArray();
    } catch (S3Exception e) {
      throw new RuntimeException("S3 exception: " + e.getMessage());
    }
  }

  private void ensureS3Client() {
    if (s3 != null) {
      return;
    }
    connect();
    if (s3 == null) {
      throw new RuntimeException("Failed to create S3 client");
    }
  }

  private void putTestObject() {
    String testInput = "UAT S3 test content";
    putObject("file1", testInput.getBytes(StandardCharsets.UTF_8));
  }

  private void getTestObject() {
    byte[] data = getObject("file1");
    log.info("got data from s3: " + new String(data, StandardCharsets.UTF_8));
  }

  private void connect() {
    String regionName = config.getString(AWS_S3_REGION);
    String endpoint;
    region = Region.of(regionName);
    bucket = config.getString(AWS_S3_BUCKET);
    URI endpointOverride;
    S3ClientBuilder s3ClientBuilder;

    endpoint = config.getString(AWS_LOCAL_ENDPOINT);
    s3ClientBuilder = S3Client.builder().region(region);
    if (environment.isDev()) {
      try {
        endpointOverride = new URI(endpoint);
        s3ClientBuilder = s3ClientBuilder.endpointOverride(endpointOverride);
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
    s3 = s3ClientBuilder.region(region).build();
  }
}
