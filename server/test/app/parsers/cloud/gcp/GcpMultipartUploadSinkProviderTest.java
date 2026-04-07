package parsers.cloud.gcp;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.googlecloud.storage.StorageObject;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import parsers.StreamingMultipartUploadResult;
import services.cloud.StorageServiceName;

public class GcpMultipartUploadSinkProviderTest {
  private static final String FILE_KEY = "test-file-key";
  private static final Integer CHUNK_SIZE = 1024 * 1024; // 1 MB

  private GcpMultipartUploadSinkProvider uploadSinkProvider;
  private Sink<ByteString, CompletionStage<StorageObject>> fakeGcpSink;
  private ActorSystem system;

  @Before
  public void setUp() {
    system = ActorSystem.create("TestSystem");

    uploadSinkProvider = spy(new GcpMultipartUploadSinkProvider());
    StorageObject mockResult = mock(StorageObject.class);
    fakeGcpSink = Sink.fold(mockResult, (acc, next) -> acc);

    when(mockResult.name()).thenReturn(FILE_KEY);
    doReturn(fakeGcpSink).when(uploadSinkProvider).getBaseSink(anyString(), anyString(), anyInt());
  }

  @After
  public void tearDown() {
    system.terminate();
  }

  @Test
  public void getUploadSink_successfulUpload_returnsGcpSuccess() throws Exception {
    StreamingMultipartUploadResult result = runSink();

    assertThat(result.getStatus()).isEqualTo(StreamingMultipartUploadResult.Status.SUCCESS);
    assertThat(result.getStorageServiceName()).isEqualTo(StorageServiceName.GCP_S3);
    assertThat(result.getStoredFilePath().get()).isEqualTo(FILE_KEY);
  }

  @Test
  public void getUploadSink_uploadFails_returnsFailure() throws Exception {
    // Set up an internal GCP failure
    RuntimeException gcpException = new RuntimeException("Access denied to GCP bucket");
    Sink<ByteString, CompletionStage<StorageObject>> failingBaseSink =
        Sink.<ByteString>cancelled()
            .mapMaterializedValue(_ -> CompletableFuture.failedFuture(gcpException));
    doReturn(failingBaseSink)
        .when(uploadSinkProvider)
        .getBaseSink(anyString(), anyString(), anyInt());

    StreamingMultipartUploadResult result = runSink();

    assertThat(result.getStatus()).isEqualTo(StreamingMultipartUploadResult.Status.FAILURE);
    assertThat(result.getStorageServiceName()).isEqualTo(StorageServiceName.GCP_S3);
    assertThat(result.getErrorMessage().get()).contains(gcpException.getMessage());
    assertThat(result.getStoredFilePath()).isEmpty();
  }

  private StreamingMultipartUploadResult runSink() throws Exception {
    Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> sink =
        checkNotNull(uploadSinkProvider.getUploadSink("bucket", FILE_KEY, CHUNK_SIZE));
    CompletionStage<StreamingMultipartUploadResult> completionStage =
        Source.single(ByteString.fromString("test")).runWith(sink, system);

    return completionStage.toCompletableFuture().get();
  }
}
