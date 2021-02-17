package repository;

import com.typesafe.config.Config;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.inject.ApplicationLifecycle;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
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
      putObject();
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

  public void putObject() {
    if (s3 == null) {
      throw new RuntimeException("S3Client is not initialized.");
    }
    s3.putObject(
        PutObjectRequest.builder().bucket(bucket).key("file1").build(),
        RequestBody.fromString("Testing with the AWS SDK for Java"));
  }

  private void connect() {
    bucket = config.getString(AWS_S3_BUCKET);

    Region region = Region.US_WEST_2;
    s3 = S3Client.builder().region(region).build();
  }
}
