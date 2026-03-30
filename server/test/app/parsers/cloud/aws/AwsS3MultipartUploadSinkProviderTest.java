package parsers.cloud.aws;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
import services.cloud.StorageServiceName;

public class AwsS3MultipartUploadSinkProviderTest {
  private static final String FILE_KEY = "test-file-key";

  private AwsS3MultipartUploadSinkProvider uploadSinkProvider;
  private Sink<ByteString, CompletionStage<MultipartUploadResult>> fakeAwsSink;
  private ActorSystem system;

  // The materializer is used under the hood here, but not called directly.
  private Materializer unused;

  @Before
  public void setUp() {
    system = ActorSystem.create("TestSystem");
    unused = Materializer.createMaterializer(system);

    uploadSinkProvider = spy(new AwsS3MultipartUploadSinkProvider());
    MultipartUploadResult mockResult = mock(MultipartUploadResult.class);
    fakeAwsSink = Sink.fold(mockResult, (acc, next) -> acc);

    when(mockResult.getKey()).thenReturn(FILE_KEY);
    when(uploadSinkProvider.getBaseSink(anyString(), anyString())).thenReturn(fakeAwsSink);
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
    doReturn(failingBaseSink).when(uploadSinkProvider).getBaseSink(anyString(), anyString());

    StreamingMultipartUploadResult result = runSink();

    assertThat(result.getStatus()).isEqualTo(StreamingMultipartUploadResult.Status.FAILURE);
    assertThat(result.getStorageServiceName()).isEqualTo(StorageServiceName.AWS_S3);
    assertThat(result.getErrorMessage().get()).contains(s3Exception.getMessage());
    assertThat(result.getStoredFilePath()).isEmpty();
  }

  private StreamingMultipartUploadResult runSink() throws Exception {
    Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> sink =
        checkNotNull(uploadSinkProvider.getUploadSink("bucket", FILE_KEY));
    CompletionStage<StreamingMultipartUploadResult> completionStage =
        Source.single(ByteString.fromString("test")).runWith(sink, system);

    return completionStage.toCompletableFuture().get();
  }
}
