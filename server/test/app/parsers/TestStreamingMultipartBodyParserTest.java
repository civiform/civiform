package parsers;

import static java.util.Arrays.fill;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static play.test.Helpers.fakeRequest;

import java.util.concurrent.CompletionStage;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Http.RequestHeader;
import repository.ResetPostgres;

public class TestStreamingMultipartBodyParserTest extends ResetPostgres {
  private static final String MULTIPART_BOUNDARY = "boundary";

  private RequestHeader requestHeader;
  private Materializer materializer;
  private StreamingOutputBuffer outputBuffer;

  private TestStreamingMultipartBodyParser parser;

  @Before
  public void setUp() {
    parser = instanceOf(TestStreamingMultipartBodyParser.class);
    materializer = instanceOf(Materializer.class);
    outputBuffer = instanceOf(StreamingOutputBuffer.class);

    requestHeader =
        fakeRequest()
            .method("POST")
            .header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY)
            .build();
  }

  @After
  public void tearDown() {
    outputBuffer.clear();
  }

  @Test
  public void testStreamingUpload_prints() throws Exception {
    Source<ByteString, ?> source = createMultipartRequestBody("Hello world");

    Http.MultipartFormData<Void> _ = parse(source).toCompletableFuture().join();

    String output = outputBuffer.getOutput();
    assertThat(output).contains("Hello world");
    assertThat(output).contains("Total size: 11 bytes");
  }

  @Test
  public void testStreamingUpload_crossesChunkBoundary_prints() throws Exception {
    int numBytes = 1024 * 1024 * 2; // 2 MB
    char[] chars = new char[numBytes];
    fill(chars, 'a');
    String data = new String(chars);
    Source<ByteString, ?> source = createMultipartRequestBody(data);

    Http.MultipartFormData<Void> _ = parse(source).toCompletableFuture().join();

    String output = outputBuffer.getOutput();
    assertThat(output).contains(data);
    assertThat(output).contains(String.format("Total size: %d bytes", numBytes));
  }

  @Test
  public void testStreamingUpload_incorrectHeader_throws() throws Exception {
    requestHeader =
        fakeRequest()
            .method("POST")
            .header(
                "Content-Type", "multipart/form-data; boundary=" + "--WRONG--" + MULTIPART_BOUNDARY)
            .build();
    Source<ByteString, ?> source = createMultipartRequestBody("Hello world");

    RuntimeException e =
        assertThrows(RuntimeException.class, () -> parse(source).toCompletableFuture().join());

    assertThat(e).hasMessageContaining("Parser failed");
  }

  // Run the parser and throw on errors
  private CompletionStage<Http.MultipartFormData<Void>> parse(Source<ByteString, ?> source) {
    return parser
        .apply(requestHeader)
        .run(source, materializer)
        .thenApply(
            either -> {
              if (either.left.isPresent()) {
                throw new RuntimeException("Parser failed with result: " + either.left.get());
              }

              return either.right.get();
            });
  }

  // Create a request body, given content
  private Source<ByteString, ?> createMultipartRequestBody(String content) {
    String requestBody =
        "--"
            + MULTIPART_BOUNDARY
            + "\r\n"
            + "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n"
            + "Content-Type: text/plain\r\n\r\n"
            + content
            + "\r\n"
            + "--"
            + MULTIPART_BOUNDARY
            + "--\r\n";
    return Source.single(ByteString.fromString(requestBody));
  }
}
