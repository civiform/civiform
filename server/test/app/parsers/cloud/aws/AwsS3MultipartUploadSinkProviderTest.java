package parsers.cloud.aws;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.typesafe.config.Config;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.s3.MultipartUploadResult;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import parsers.StreamingMultipartUploadResult;
import repository.ResetPostgres;
import services.cloud.BucketType;
import services.cloud.StorageServiceName;

public class AwsS3MultipartUploadSinkProviderTest extends ResetPostgres {
  private static final String FILE_KEY = "test-file-key";
  private static final Integer CHUNK_SIZE = 1024 * 1024; // 1 MB

  private AwsS3MultipartUploadSinkProvider uploadSinkProvider;
  private Sink<ByteString, CompletionStage<MultipartUploadResult>> fakeAwsSink;
  private ActorSystem system;

  @Before
  public void setUp() {
    system = ActorSystem.create("TestSystem");

    uploadSinkProvider = spy(new AwsS3MultipartUploadSinkProvider(instanceOf(Config.class)));
    MultipartUploadResult mockResult = mock(MultipartUploadResult.class);
    fakeAwsSink = Sink.fold(mockResult, (acc, next) -> acc);

    when(mockResult.getKey()).thenReturn(FILE_KEY);
    doReturn(fakeAwsSink)
        .when(uploadSinkProvider)
        .getBaseSink(any(BucketType.class), anyString(), anyInt());
  }

  @After
  public void tearDown() {
    system.terminate();
  }

  @Test
  public void getUploadSink_successfulUpload_returnsAwsSuccess() throws Exception {
    StreamingMultipartUploadResult result = runSink();

    assertThat(result.getStatus()).isEqualTo(StreamingMultipartUploadResult.Status.SUCCESS);
    assertThat(result.getStorageServiceName()).isEqualTo(StorageServiceName.AWS_S3);
    assertThat(result.getStoredFilePath().get()).isEqualTo(FILE_KEY);
  }

  @Test
  public void getUploadSink_uploadFails_returnsFailure() throws Exception {
    // Set up an internal AWS failure
    RuntimeException s3Exception = new RuntimeException("Access denied to S3 bucket");
    Sink<ByteString, CompletionStage<MultipartUploadResult>> failingBaseSink =
        Sink.<ByteString>cancelled()
            .mapMaterializedValue(_ -> CompletableFuture.failedFuture(s3Exception));
    doReturn(failingBaseSink)
        .when(uploadSinkProvider)
        .getBaseSink(any(BucketType.class), anyString(), anyInt());

    StreamingMultipartUploadResult result = runSink();

    assertThat(result.getStatus()).isEqualTo(StreamingMultipartUploadResult.Status.FAILURE);
    assertThat(result.getStorageServiceName()).isEqualTo(StorageServiceName.AWS_S3);
    assertThat(result.getErrorMessage().get()).contains(s3Exception.getMessage());
    assertThat(result.getStoredFilePath()).isEmpty();
  }

  private StreamingMultipartUploadResult runSink() throws Exception {
    Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> sink =
        checkNotNull(
            uploadSinkProvider.getUploadSink(BucketType.PRIVATE_BUCKET, FILE_KEY, CHUNK_SIZE));
    CompletionStage<StreamingMultipartUploadResult> completionStage =
        Source.single(ByteString.fromString("test")).runWith(sink, system);

    return completionStage.toCompletableFuture().get();
  }
}
