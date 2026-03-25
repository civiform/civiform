package parsers.cloud;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException.BadValue;
import com.typesafe.config.ConfigFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.connectors.s3.MultipartUploadResult;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import parsers.StreamingMultipartUploadResult;
import repository.ResetPostgres;
import services.cloud.StorageServiceName;

public class StreamingMultipartUploadSinksTest extends ResetPostgres {
  private static final String FILE_KEY = "test-file-key";

  private Sink<ByteString, CompletionStage<MultipartUploadResult>> fakeAwsSink;
  private StreamingMultipartUploadSinks uploadSinks;
  private Config config;
  private ActorSystem system;

  // The materializer is used under the hood here, but not called directly.
  private Materializer unused;

  @Before
  public void setUp() {
    system = ActorSystem.create("TestSystem");
    unused = Materializer.createMaterializer(system);
    MultipartUploadResult mockResult = mock(MultipartUploadResult.class);
    when(mockResult.getKey()).thenReturn(FILE_KEY);
    fakeAwsSink = Sink.fold(mockResult, (acc, next) -> acc);
  }

  @After
  public void tearDown() {
    system.terminate();
  }

  @Test
  public void getSinkForCloudProvider_awsS3_returnsAwsSink() throws Exception {
    createSpiedUploadSink(StorageServiceName.AWS_S3.getString());

    StreamingMultipartUploadResult result = runSink();

    assertThat(result.getStatus()).isEqualTo(StreamingMultipartUploadResult.Status.SUCCESS);
    assertThat(result.getStorageServiceName()).isEqualTo(StorageServiceName.AWS_S3);
    assertThat(result.getStoredFilePath().get()).isEqualTo(FILE_KEY);
  }

  @Test
  public void getSinkForCloudProvider_awsS3_uploadFails_returnsFailure() throws Exception {
    // Set up an internal AWS failure
    RuntimeException s3Exception = new RuntimeException("Access denied to S3 bucket");
    Sink<ByteString, CompletionStage<MultipartUploadResult>> failingBaseSink =
        Sink.<ByteString>cancelled()
            .mapMaterializedValue(_ -> CompletableFuture.failedFuture(s3Exception));
    createSpiedUploadSink(StorageServiceName.AWS_S3.getString());
    doReturn(failingBaseSink).when(uploadSinks).getBaseS3Sink(anyString(), anyString());

    StreamingMultipartUploadResult result = runSink();

    assertThat(result.getStatus()).isEqualTo(StreamingMultipartUploadResult.Status.FAILURE);
    assertThat(result.getStorageServiceName()).isEqualTo(StorageServiceName.AWS_S3);
    assertThat(result.getErrorMessage().get()).contains(s3Exception.getMessage());
    assertThat(result.getStoredFilePath()).isEmpty();
  }

  @Test
  public void getSinkForCloudProvider_azure_returnsInvalid() throws Exception {
    createSpiedUploadSink(StorageServiceName.AZURE_BLOB.getString());

    StreamingMultipartUploadResult result = runSink();

    assertThat(result.getStatus()).isEqualTo(StreamingMultipartUploadResult.Status.INVALID_STATUS);
    assertThat(result.getStorageServiceName()).isEqualTo(StorageServiceName.AZURE_BLOB);
    assertThat(result.getStoredFilePath()).isEmpty();
  }

  @Test
  public void getSinkForCloudProvider_invalidCloudProviderConfigured_throws() throws Exception {
    createSpiedUploadSink("AN_ACTUAL_CLOUD");

    BadValue e = assertThrows(BadValue.class, () -> runSink());

    assertThat(e).hasMessageContaining("AN_ACTUAL_CLOUD is not a valid storage provider.");
  }

  private StreamingMultipartUploadResult runSink() throws Exception {
    Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> sink =
        checkNotNull(uploadSinks).getSinkForCloudProvider("bucket", FILE_KEY);
    CompletionStage<StreamingMultipartUploadResult> completionStage =
        Source.single(ByteString.fromString("test")).runWith(sink, system);

    return completionStage.toCompletableFuture().get();
  }

  private void createSpiedUploadSink(String storageProvider) {
    config = ConfigFactory.parseMap(ImmutableMap.of("cloud.storage", storageProvider));
    uploadSinks = spy(new StreamingMultipartUploadSinks(config));
    doReturn(fakeAwsSink).when(uploadSinks).getBaseS3Sink(anyString(), anyString());
  }
}
