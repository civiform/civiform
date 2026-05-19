package parsers.cloud;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException.BadValue;
import com.typesafe.config.ConfigFactory;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import parsers.StreamingMultipartUploadResult;
import parsers.cloud.aws.AwsS3MultipartUploadSinkProvider;
import parsers.cloud.gcp.GcpMultipartUploadSinkProvider;
import services.cloud.BucketType;
import services.cloud.StorageServiceName;

public class MultipartUploadSinksTest {
  private static final String FILE_KEY = "test-file-key";
  private static final Integer CHUNK_SIZE = 1024 * 1024; // 1 MB

  private MultipartUploadSinks uploadSinks;
  private Config config;
  private ActorSystem system;

  @Before
  public void setUp() {
    system = ActorSystem.create("TestSystem");
  }

  @After
  public void tearDown() {
    system.terminate();
  }

  @Test
  public void getSinkForCloudProvider_awsS3_returnsAwsSink() throws Exception {
    createUploadSink(StorageServiceName.AWS_S3.getString());

    assertThat(uploadSinks.getUploadSinkProvider().getClass())
        .isEqualTo(AwsS3MultipartUploadSinkProvider.class);
  }

  @Test
  public void getSinkForCloudProvider_gcp_returnsGcpSink() throws Exception {
    createUploadSink(StorageServiceName.GCP_S3.getString());

    assertThat(uploadSinks.getUploadSinkProvider().getClass())
        .isEqualTo(GcpMultipartUploadSinkProvider.class);
  }

  @Test
  public void getSinkForCloudProvider_azure_returnsInvalid() throws Exception {
    createUploadSink(StorageServiceName.AZURE_BLOB.getString());

    StreamingMultipartUploadResult result = runSink();

    assertThat(result.getStatus()).isEqualTo(StreamingMultipartUploadResult.Status.NOT_IMPLEMENTED);
    assertThat(result.getStorageServiceName()).isEqualTo(StorageServiceName.AZURE_BLOB);
    assertThat(result.getStoredFilePath()).isEmpty();
  }

  @Test
  public void getSinkForCloudProvider_invalidCloudProviderConfigured_throws() throws Exception {
    BadValue e = assertThrows(BadValue.class, () -> createUploadSink("AN_ACTUAL_CLOUD"));

    assertThat(e).hasMessageContaining("AN_ACTUAL_CLOUD is not a valid storage provider.");
  }

  @Test
  public void getMaxUploadSizeBytes_awsS3_returnsLimitsFromConfigByBucketType() {
    createUploadSink(StorageServiceName.AWS_S3.getString());

    assertThat(uploadSinks.getMaxUploadSizeBytes(BucketType.PRIVATE_BUCKET))
        .isEqualTo(100L * 1024L * 1024L);
    assertThat(uploadSinks.getMaxUploadSizeBytes(BucketType.PUBLIC_BUCKET))
        .isEqualTo(1L * 1024L * 1024L);
  }

  @Test
  public void getMaxUploadSizeBytes_gcp_returnsLimitsFromConfigByBucketType() {
    createUploadSink(StorageServiceName.GCP_S3.getString());

    assertThat(uploadSinks.getMaxUploadSizeBytes(BucketType.PRIVATE_BUCKET))
        .isEqualTo(50L * 1024L * 1024L);
    assertThat(uploadSinks.getMaxUploadSizeBytes(BucketType.PUBLIC_BUCKET))
        .isEqualTo(2L * 1024L * 1024L);
  }

  @Test
  public void getMaxUploadSizeBytes_azure_returnsLimitsFromConfigByBucketType() {
    createUploadSink(StorageServiceName.AZURE_BLOB.getString());

    assertThat(uploadSinks.getMaxUploadSizeBytes(BucketType.PRIVATE_BUCKET))
        .isEqualTo(80L * 1024L * 1024L);
    assertThat(uploadSinks.getMaxUploadSizeBytes(BucketType.PUBLIC_BUCKET))
        .isEqualTo(3L * 1024L * 1024L);
  }

  private StreamingMultipartUploadResult runSink() throws Exception {
    Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> sink =
        checkNotNull(uploadSinks)
            .getSinkForCloudProvider(BucketType.PRIVATE_BUCKET, FILE_KEY, CHUNK_SIZE);
    CompletionStage<StreamingMultipartUploadResult> completionStage =
        Source.single(ByteString.fromString("test")).runWith(sink, system);

    return completionStage.toCompletableFuture().get();
  }

  private void createUploadSink(String storageProvider) {
    config =
        ConfigFactory.parseMap(
            ImmutableMap.<String, Object>builder()
                .put("cloud.storage", storageProvider)
                .put("aws.s3.bucket", "test-aws-bucket")
                .put("aws.s3.public_bucket", "test-aws-public-bucket")
                .put("aws.s3.filelimitmb", 100)
                .put("aws.s3.public_file_limit_mb", 1)
                .put("gcp.s3.bucket", "test-gcp-bucket")
                .put("gcp.s3.public_bucket", "test-gcp-public-bucket")
                .put("gcp.s3.filelimitmb", 50)
                .put("gcp.s3.public_file_limit_mb", 2)
                .put("azure.blob.file_limit_mb", 80)
                .put("azure.blob.public_file_limit_mb", 3)
                .build());
    uploadSinks = new MultipartUploadSinks(config);
  }
}
