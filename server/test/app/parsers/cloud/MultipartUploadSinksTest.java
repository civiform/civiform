package parsers.cloud;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.spy;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException.BadValue;
import com.typesafe.config.ConfigFactory;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import parsers.StreamingMultipartUploadResult;
import parsers.cloud.aws.AwsS3MultipartUploadSinkProvider;
import repository.ResetPostgres;
import services.cloud.StorageServiceName;

public class MultipartUploadSinksTest extends ResetPostgres {
  private static final String FILE_KEY = "test-file-key";

  private MultipartUploadSinks uploadSinks;
  private Config config;
  private ActorSystem system;

  // The materializer is used under the hood here, but not called directly.
  private Materializer unused;

  @Before
  public void setUp() {
    system = ActorSystem.create("TestSystem");
    unused = Materializer.createMaterializer(system);
  }

  @After
  public void tearDown() {
    system.terminate();
  }

  @Test
  public void getSinkForCloudProvider_awsS3_returnsAwsSink() throws Exception {
    createSpiedUploadSink(StorageServiceName.AWS_S3.getString());

    assertThat(uploadSinks.getUploadSinkProvider().getClass())
        .isEqualTo(AwsS3MultipartUploadSinkProvider.class);
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
    BadValue e = assertThrows(BadValue.class, () -> createSpiedUploadSink("AN_ACTUAL_CLOUD"));

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
    uploadSinks = spy(new MultipartUploadSinks(config));
  }
}
