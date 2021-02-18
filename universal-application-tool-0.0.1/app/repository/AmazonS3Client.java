package repository;

import com.typesafe.config.Config;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.inject.ApplicationLifecycle;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Singleton
public class AmazonS3Client {
  public static final String AWS_S3_BUCKET = "aws.s3.bucket";

  private final ApplicationLifecycle appLifecycle;
  private final Config config;
  private String bucket;
  private S3Client s3;

  @Inject
  public AmazonS3Client(ApplicationLifecycle appLifecycle, Config config) {
    this.appLifecycle = appLifecycle;
    this.config = config;

    System.out.println("aws s3 enabled: " + String.valueOf(enabled()));
    if (enabled()) {
      connect();
      putTestObject();
      getTestObject();
    }

    appLifecycle.addStopHook(
        () -> {
          if (s3 != null) {
            s3.close();
          }
          return CompletableFuture.completedFuture(null);
        });
  }

  public boolean enabled() {
    return config.hasPath(AWS_S3_BUCKET);
  }

  public void putObject(String key, byte[] data) {
    throwIfUninitialized();

    PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucket).key(key).build();
    s3.putObject(putObjectRequest, RequestBody.fromBytes(data));
  }

  public byte[] getObject(String key) {
    throwIfUninitialized();

    GetObjectRequest getObjectRequest = GetObjectRequest.builder().key(key).bucket(bucket).build();
    ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(getObjectRequest);
    return objectBytes.asByteArray();
  }

  private void throwIfUninitialized() {
    if (s3 == null) {
      throw new RuntimeException("S3Client is not initialized.");
    }
  }

  private void putTestObject() {
    String testInput = "UAT S3 test content";
    putObject("file1", testInput.getBytes());
  }

  private void getTestObject() {
    byte[] data = getObject("file1");
    System.out.println("got data from s3: " + new String(data));
  }

  private void connect() {
    bucket = config.getString(AWS_S3_BUCKET);

    Region region = Region.US_WEST_2;
    s3 = S3Client.builder().region(region).build();
  }
}
