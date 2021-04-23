package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
  private Region region;
  private String bucket;
  private S3Client s3;
  private S3Presigner presigner;

  public AmazonS3Client() {
    this.appLifecycle = null;
    this.config = null;
    this.environment = null;
    this.s3 = null;
    this.presigner = null;
  }

  @Inject
  public AmazonS3Client(ApplicationLifecycle appLifecycle, Config config, Environment environment) {
    this.appLifecycle = checkNotNull(appLifecycle);
    this.config = checkNotNull(config);
    this.environment = checkNotNull(environment);

    log.info("aws s3 enabled: " + String.valueOf(enabled()));
    if (enabled()) {
      ensureS3Client();

      if (enabled()) {
        putTestObject();
        getTestObject();
      }
    }
    addStopHook();
  }

  public boolean enabled() {
    if (!config.hasPath(AWS_ENABLE) || !config.getBoolean(AWS_ENABLE)) {
      return false;
    }
    return (config.hasPath(AWS_S3_REGION) && config.hasPath(AWS_S3_BUCKET));
  }

  public boolean connected() {
    return s3 != null;
  }

  public void putObject(String key, byte[] data) {
    int i = 0;

    while (true) {
      try {
        putObjectInner(key, data);
        break;
      } catch (RuntimeException e) {
        if (i < 3) {
          sleep(50 * (int) Math.pow(2, i));
        } else {
          throw e;
        }
      }

      i++;
    }
  }

  public byte[] getObject(String key) {
    int i = 0;

    byte[] byteArray;

    while (true) {
      try {
        byteArray = getObjectInner(key);
        return byteArray;
      } catch (RuntimeException e) {
        if (i < 3) {
          sleep(50 * (int) Math.pow(2, i));
        } else {
          throw e;
        }
      }

      i++;
    }
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

  public void ensureS3Client() {
    if (s3 != null) {
      return;
    }

    connectS3();
    connectPresigner();

    if (s3 == null) {
      throw new RuntimeException("Failed to create S3 client");
    }

    if (presigner == null) {
      throw new RuntimeException("Failed to create S3 client presigner");
    }
  }

  private void putObjectInner(String key, byte[] data) {
    ensureS3Client();

    try {
      PutObjectRequest putObjectRequest =
            PutObjectRequest.builder().bucket(bucket).key(key).build();
        s3.putObject(putObjectRequest, RequestBody.fromBytes(data));
      } catch (S3Exception e) {
        throw new RuntimeException("S3 exception: " + e.getMessage());
    }
  }

  private void sleep(int t) {
    try {
      Thread.sleep(t);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupt on Thread.sleep: Aborting S3 bucket connection");
    }
  }

  private void addStopHook() {
    this.appLifecycle.addStopHook(
        () -> {
          if (s3 != null) {
            s3.close();
          }
          return CompletableFuture.completedFuture(null);
        });
  }

  private byte[] getObjectInner(String key) {
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

  private void putTestObject() {
    String testInput = "UAT S3 test content";
    putObject("file1", testInput.getBytes(StandardCharsets.UTF_8));
  }

  private void getTestObject() {
    byte[] data = getObject("file1");
    log.info("got data from s3: " + new String(data, StandardCharsets.UTF_8));
  }

  private void connectS3() {
    if (s3 != null) {
      return;
    }

    String regionName = config.getString(AWS_S3_REGION);
    region = Region.of(regionName);
    bucket = config.getString(AWS_S3_BUCKET);

    S3ClientBuilder s3ClientBuilder = S3Client.builder().region(region);
    if (environment.isDev()) {
      try {
        URI localUri = new URI(config.getString(AWS_LOCAL_ENDPOINT));
        s3ClientBuilder = s3ClientBuilder.endpointOverride(localUri);
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
    s3 = s3ClientBuilder.build();
  }

  private void connectPresigner() {
    if (presigner != null) {
      return;
    }

    String regionName = config.getString(AWS_S3_REGION);
    region = Region.of(regionName);
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
