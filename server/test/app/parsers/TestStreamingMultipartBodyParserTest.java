package parsers;

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
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.RequestHeader;
import repository.ResetPostgres;

public class TestStreamingMultipartBodyParserTest extends ResetPostgres {
  private static final String MULTIPART_BOUNDARY = "boundary";
  // Valid PNG header bytes for tests that need to pass file type validation
  private static final byte[] PNG_HEADER = {
    (byte) 0x89,
    0x50,
    0x4E,
    0x47,
    0x0D,
    0x0A,
    0x1A,
    0x0A,
    0x00,
    0x00,
    0x00,
    0x0D,
    0x49,
    0x48,
    0x44,
    0x52
  };

  private RequestHeader requestHeader;
  private Materializer materializer;
  private parsers.StreamingOutputBuffer outputBuffer;

  private parsers.TestStreamingMultipartBodyParser parser;

  @Before
  public void setUp() {
    parser = instanceOf(parsers.TestStreamingMultipartBodyParser.class);
    materializer = instanceOf(Materializer.class);
    outputBuffer = instanceOf(parsers.StreamingOutputBuffer.class);

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
  public void testStreamingUpload_incorrectHeader_throws() throws Exception {
    requestHeader =
        fakeRequest()
            .method("POST")
            .header(
                "Content-Type", "multipart/form-data; boundary=" + "--WRONG--" + MULTIPART_BOUNDARY)
            .build();
    Source<ByteString, ?> source =
        createMultipartRequestBodyWithBytes(PNG_HEADER, "test.png", "image/png");

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

  @Test
  public void testStreamingUpload_validPng_succeeds() throws Exception {
    byte[] pngHeader = {
      (byte) 0x89,
      0x50,
      0x4E,
      0x47,
      0x0D,
      0x0A,
      0x1A,
      0x0A,
      0x00,
      0x00,
      0x00,
      0x0D,
      0x49,
      0x48,
      0x44,
      0x52,
      0x00,
      0x00,
      0x00,
      0x01
    };
    Source<ByteString, ?> source =
        createMultipartRequestBodyWithBytes(pngHeader, "test.png", "image/png");

    MultipartFormData<Void> result = parse(source).toCompletableFuture().join();

    assertThat(result).isNotNull();
  }

  @Test
  public void testStreamingUpload_validPdf_succeeds() throws Exception {
    byte[] pdfHeader = {
      0x25,
      0x50,
      0x44,
      0x46,
      0x2D,
      0x31,
      0x2E,
      0x34,
      0x0A,
      0x25,
      (byte) 0xC3,
      (byte) 0xA4,
      (byte) 0xC3,
      (byte) 0xBC,
      (byte) 0xC3,
      (byte) 0xB6,
      0x0A,
      0x31
    };
    Source<ByteString, ?> source =
        createMultipartRequestBodyWithBytes(pdfHeader, "doc.pdf", "application/pdf");

    MultipartFormData<Void> result = parse(source).toCompletableFuture().join();

    assertThat(result).isNotNull();
  }

  @Test
  public void testStreamingUpload_mismatchedType_throws() throws Exception {
    // PNG magic bytes but declared as application/pdf
    byte[] pngHeader = {
      (byte) 0x89,
      0x50,
      0x4E,
      0x47,
      0x0D,
      0x0A,
      0x1A,
      0x0A,
      0x00,
      0x00,
      0x00,
      0x0D,
      0x49,
      0x48,
      0x44,
      0x52,
      0x00,
      0x00,
      0x00,
      0x01
    };
    Source<ByteString, ?> source =
        createMultipartRequestBodyWithBytes(pngHeader, "fake.pdf", "application/pdf");

    RuntimeException e =
        assertThrows(RuntimeException.class, () -> parse(source).toCompletableFuture().join());

    assertThat(e).hasMessageContaining("does not match detected type");
  }

  @Test
  public void testStreamingUpload_disallowedType_throws() throws Exception {
    byte[] unknownHeader = {
      0x4D, 0x5A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00
    };
    Source<ByteString, ?> source =
        createMultipartRequestBodyWithBytes(
            unknownHeader, "malware.exe", "application/x-executable");

    RuntimeException e =
        assertThrows(RuntimeException.class, () -> parse(source).toCompletableFuture().join());

    assertThat(e).hasMessageContaining("not an allowed upload type");
  }

  private Source<ByteString, ?> createMultipartRequestBodyWithBytes(
      byte[] content, String filename, String contentType) {
    String header =
        "--"
            + MULTIPART_BOUNDARY
            + "\r\n"
            + "Content-Disposition: form-data; name=\"file\"; filename=\""
            + filename
            + "\"\r\n"
            + "Content-Type: "
            + contentType
            + "\r\n\r\n";
    String footer = "\r\n--" + MULTIPART_BOUNDARY + "--\r\n";

    ByteString body =
        ByteString.fromString(header)
            .concat(ByteString.fromArray(content))
            .concat(ByteString.fromString(footer));
    return Source.single(body);
  }
}
