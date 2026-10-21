package parsers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.junit.Before;
import org.junit.Test;
import parsers.FileTypeValidation;
import parsers.FileUploadTypeException;
import parsers.StreamingMultipartUploadResult;
import parsers.cloud.MultipartUploadSinks;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http;
import repository.ResetPostgres;
import services.cloud.BucketType;
import services.cloud.PublicFileNameFormatter;
import services.cloud.StorageServiceName;

public class QuestionImageStreamingMultipartBodyParserTest extends ResetPostgres {
  private static final String MULTIPART_BOUNDARY = "boundary";
  private static final long QUESTION_ID = 7L;

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

  // Valid JPEG header bytes (JFIF), at least 16 bytes for FileTypeValidation
  private static final byte[] JPEG_HEADER = {
    (byte) 0xFF,
    (byte) 0xD8,
    (byte) 0xFF,
    (byte) 0xE0,
    0x00,
    0x10,
    0x4A,
    0x46,
    0x49,
    0x46,
    0x00,
    0x01,
    0x01,
    0x01,
    0x00,
    0x60
  };

  // Valid PDF header bytes (%PDF-1.4), at least 16 bytes for FileTypeValidation
  private static final byte[] PDF_HEADER = {
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

  private QuestionImageStreamingMultipartBodyParser parser;
  private Materializer materializer;
  private MultipartUploadSinks sinks;

  @Before
  public void setUp() {
    materializer = instanceOf(Materializer.class);
    DefaultHttpErrorHandler errorHandler = instanceOf(DefaultHttpErrorHandler.class);

    sinks = mock(MultipartUploadSinks.class);
    when(sinks.getSinkForCloudProvider(any(BucketType.class), anyString(), any(), any(), anyInt()))
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
        new QuestionImageStreamingMultipartBodyParser(
            materializer, errorHandler, sinks, instanceOf(FileTypeValidation.class));
  }

  @Test
  public void maxFileSize_isOneMegabyte() {
    assertThat(QuestionImageStreamingMultipartBodyParser.MAX_FILE_SIZE)
        .isEqualTo(1L * 1024L * 1024L);
  }

  @Test
  public void
      streamingUpload_pngImage_streamsToPublicBucketWithQuestionImageFileKeyAndInlineDisposition()
          throws Exception {
    Http.RequestHeader request =
        fakeRequest()
            .method("POST")
            .uri(String.format("/admin/questions/%d/hx/image/upload", QUESTION_ID))
            .header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY)
            .build();

    Source<ByteString, ?> source = createMultipartRequestBody("hello.png", "image/png", PNG_HEADER);

    CompletionStage<play.libs.F.Either<play.mvc.Result, Http.MultipartFormData<String>>> stage =
        parser.apply(request).run(source, materializer);
    Http.MultipartFormData<String> body = stage.toCompletableFuture().join().right.get();

    Http.MultipartFormData.FilePart<String> filePart = body.getFile("questionImage");
    assertThat(filePart).isNotNull();
    assertThat(filePart.getFilename()).isEqualTo("hello.png");
    assertThat(filePart.getContentType()).isEqualTo("image/png");

    String fileKey = filePart.getRef();
    assertThat(fileKey)
        .isEqualTo(
            PublicFileNameFormatter.formatPublicQuestionImageFileKey(QUESTION_ID, "hello.png"));
    assertThat(PublicFileNameFormatter.isFileKeyForPublicQuestionImage(fileKey)).isTrue();

    verify(sinks)
        .getSinkForCloudProvider(
            eq(BucketType.PUBLIC_BUCKET),
            eq(fileKey),
            eq(Optional.of("image/png")),
            eq(Optional.of("inline")),
            anyInt());
  }

  @Test
  public void
      streamingUpload_jpegImage_streamsToPublicBucketWithQuestionImageFileKeyAndInlineDisposition()
          throws Exception {
    Http.RequestHeader request =
        fakeRequest()
            .method("POST")
            .uri(String.format("/admin/questions/%d/hx/image/upload", QUESTION_ID))
            .header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY)
            .build();

    Source<ByteString, ?> source =
        createMultipartRequestBody("sample.jpg", "image/jpeg", JPEG_HEADER);

    CompletionStage<play.libs.F.Either<play.mvc.Result, Http.MultipartFormData<String>>> stage =
        parser.apply(request).run(source, materializer);
    Http.MultipartFormData<String> body = stage.toCompletableFuture().join().right.get();

    Http.MultipartFormData.FilePart<String> filePart = body.getFile("questionImage");
    assertThat(filePart).isNotNull();
    assertThat(filePart.getFilename()).isEqualTo("sample.jpg");
    assertThat(filePart.getContentType()).isEqualTo("image/jpeg");

    String fileKey = filePart.getRef();
    assertThat(fileKey)
        .isEqualTo(
            PublicFileNameFormatter.formatPublicQuestionImageFileKey(QUESTION_ID, "sample.jpg"));
    assertThat(PublicFileNameFormatter.isFileKeyForPublicQuestionImage(fileKey)).isTrue();

    verify(sinks)
        .getSinkForCloudProvider(
            eq(BucketType.PUBLIC_BUCKET),
            eq(fileKey),
            eq(Optional.of("image/jpeg")),
            eq(Optional.of("inline")),
            anyInt());
  }

  @Test
  public void streamingUpload_requestPathWithTrailingSlash_extractsQuestionIdCorrectly()
      throws Exception {
    long questionId = 99L;
    Http.RequestHeader request =
        fakeRequest()
            .method("POST")
            .uri(String.format("/admin/questions/%d/hx/image/upload/", questionId))
            .header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY)
            .build();

    Source<ByteString, ?> source = createMultipartRequestBody("test.png", "image/png", PNG_HEADER);

    CompletionStage<play.libs.F.Either<play.mvc.Result, Http.MultipartFormData<String>>> stage =
        parser.apply(request).run(source, materializer);
    Http.MultipartFormData<String> body = stage.toCompletableFuture().join().right.get();

    Http.MultipartFormData.FilePart<String> filePart = body.getFile("questionImage");
    assertThat(filePart).isNotNull();
    assertThat(filePart.getRef())
        .isEqualTo(
            PublicFileNameFormatter.formatPublicQuestionImageFileKey(questionId, "test.png"));
  }

  @Test
  public void streamingUpload_invalidRequestPath_throwsIllegalStateException() {
    Http.RequestHeader request =
        fakeRequest()
            .method("POST")
            .uri("/admin/questions/not-a-number/hx/image/upload")
            .header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY)
            .build();

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> parser.apply(request));
    assertThat(exception)
        .hasMessageContaining("Request path does not match expected question image upload pattern");
  }

  @Test
  public void streamingUpload_disallowedFileType_fails() {
    Http.RequestHeader request =
        fakeRequest()
            .method("POST")
            .uri(String.format("/admin/questions/%d/hx/image/upload", QUESTION_ID))
            .header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY)
            .build();

    Source<ByteString, ?> source =
        createMultipartRequestBody("doc.pdf", "application/pdf", PDF_HEADER);

    CompletionStage<play.libs.F.Either<play.mvc.Result, Http.MultipartFormData<String>>> stage =
        parser.apply(request).run(source, materializer);

    assertThatThrownBy(() -> stage.toCompletableFuture().join())
        .hasCauseInstanceOf(FileUploadTypeException.class)
        .hasMessageContaining("not an allowed upload type");
  }

  private Source<ByteString, ?> createMultipartRequestBody(
      String filename, String contentType, byte[] content) {
    ByteString header =
        ByteString.fromString(
            "--"
                + MULTIPART_BOUNDARY
                + "\r\n"
                + "Content-Disposition: form-data; name=\"questionImage\"; filename=\""
                + filename
                + "\"\r\n"
                + "Content-Type: "
                + contentType
                + "\r\n\r\n");
    ByteString footer = ByteString.fromString("\r\n--" + MULTIPART_BOUNDARY + "--\r\n");
    return Source.single(header.concat(ByteString.fromArray(content)).concat(footer));
  }
}
