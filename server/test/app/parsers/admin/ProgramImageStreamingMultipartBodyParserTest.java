package parsers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;

import java.util.concurrent.CompletionStage;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.junit.Before;
import org.junit.Test;
import parsers.FileTypeValidation;
import parsers.StreamingMultipartUploadResult;
import parsers.cloud.MultipartUploadSinks;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http;
import repository.ResetPostgres;
import services.cloud.BucketType;
import services.cloud.PublicFileNameFormatter;
import services.cloud.StorageServiceName;

public class ProgramImageStreamingMultipartBodyParserTest extends ResetPostgres {
  private static final String MULTIPART_BOUNDARY = "boundary";
  private static final long PROGRAM_ID = 7L;
  private static final String EDIT_STATUS = "EDIT";
  // Valid PNG header bytes, at least 16 bytes for FileTypeValidation
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

  private ProgramImageStreamingMultipartBodyParser parser;
  private Materializer materializer;
  private MultipartUploadSinks sinks;

  @Before
  public void setUp() {
    materializer = instanceOf(Materializer.class);
    DefaultHttpErrorHandler errorHandler = instanceOf(DefaultHttpErrorHandler.class);

    sinks = mock(MultipartUploadSinks.class);
    when(sinks.getSinkForCloudProvider(any(BucketType.class), anyString(), anyInt()))
        .thenAnswer(
            invocation ->
                Sink.<ByteString, ByteString>fold(ByteString.emptyByteString(), ByteString::concat)
                    .mapMaterializedValue(
                        stage ->
                            stage.thenApply(
                                bytes ->
                                    StreamingMultipartUploadResult.builder()
                                        .setStatus(StreamingMultipartUploadResult.Status.SUCCESS)
                                        .setStorageServiceName(StorageServiceName.S3)
                                        .build())));

    parser =
        new ProgramImageStreamingMultipartBodyParser(
            materializer, errorHandler, sinks, instanceOf(FileTypeValidation.class));
  }

  @Test
  public void maxFileSize_isOneMegabyte() {
    assertThat(ProgramImageStreamingMultipartBodyParser.MAX_FILE_SIZE)
        .isEqualTo(1L * 1024L * 1024L);
  }

  @Test
  public void streamingUpload_streamsToPublicBucketWithProgramImageFileKey() throws Exception {
    Http.RequestHeader request =
        fakeRequest()
            .method("POST")
            .uri(String.format("/admin/programs/%d/image/upload/%s", PROGRAM_ID, EDIT_STATUS))
            .header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY)
            .build();

    Source<ByteString, ?> source = createMultipartRequestBody("hello.png", PNG_HEADER);

    CompletionStage<play.libs.F.Either<play.mvc.Result, Http.MultipartFormData<String>>> stage =
        parser.apply(request).run(source, materializer);
    Http.MultipartFormData<String> body = stage.toCompletableFuture().join().right.get();

    Http.MultipartFormData.FilePart<String> filePart = body.getFile("file");
    assertThat(filePart).isNotNull();
    assertThat(filePart.getFilename()).isEqualTo("hello.png");
    assertThat(filePart.getContentType()).isEqualTo("image/png");

    String fileKey = filePart.getRef();
    assertThat(fileKey)
        .isEqualTo(
            PublicFileNameFormatter.formatPublicProgramImageFileKey(PROGRAM_ID, "hello.png"));

    verify(sinks).getSinkForCloudProvider(eq(BucketType.PUBLIC_BUCKET), eq(fileKey), anyInt());
  }

  private Source<ByteString, ?> createMultipartRequestBody(String filename, byte[] content) {
    ByteString header =
        ByteString.fromString(
            "--"
                + MULTIPART_BOUNDARY
                + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\""
                + filename
                + "\"\r\n"
                + "Content-Type: image/png\r\n\r\n");
    ByteString footer = ByteString.fromString("\r\n--" + MULTIPART_BOUNDARY + "--\r\n");
    return Source.single(header.concat(ByteString.fromArray(content)).concat(footer));
  }
}
