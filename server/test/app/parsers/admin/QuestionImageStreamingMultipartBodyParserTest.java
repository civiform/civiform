package parsers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import java.util.Optional;
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
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;
import repository.ResetPostgres;
import services.cloud.BucketType;
import services.cloud.PublicFileNameFormatter;
import services.cloud.StorageServiceName;

public class QuestionImageStreamingMultipartBodyParserTest extends ResetPostgres {
  private static final String MULTIPART_BOUNDARY = "boundary";
  private static final long QUESTION_ID = 42L;
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

  private QuestionImageStreamingMultipartBodyParser parser;
  private Materializer materializer;
  private MultipartUploadSinks sinks;
  private ProfileUtils profileUtils;
  private CiviFormProfile profile;

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

    profileUtils = mock(ProfileUtils.class);
    profile = mock(CiviFormProfile.class);
    when(profile.isCiviFormAdmin()).thenReturn(true);
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(profile));

    parser =
        new QuestionImageStreamingMultipartBodyParser(
            materializer, errorHandler, sinks, instanceOf(FileTypeValidation.class), profileUtils);
  }

  @Test
  public void maxFileSize_isOneMegabyte() {
    assertThat(QuestionImageStreamingMultipartBodyParser.MAX_FILE_SIZE)
        .isEqualTo(1L * 1024L * 1024L);
  }

  @Test
  public void streamingUpload_streamsToPublicBucketWithQuestionImageFileKey() throws Exception {
    Source<ByteString, ?> source = createMultipartRequestBody("hello.png", PNG_HEADER);

    Http.MultipartFormData<String> body = parse(source).right.get();

    Http.MultipartFormData.FilePart<String> filePart = body.getFile("file");
    assertThat(filePart).isNotNull();
    assertThat(filePart.getFilename()).isEqualTo("hello.png");
    assertThat(filePart.getContentType()).isEqualTo("image/png");

    String fileKey = filePart.getRef();
    assertThat(fileKey)
        .isEqualTo(
            PublicFileNameFormatter.formatPublicQuestionImageFileKey(QUESTION_ID, "hello.png"));

    verify(sinks).getSinkForCloudProvider(eq(BucketType.PUBLIC_BUCKET), eq(fileKey), anyInt());
  }

  @Test
  public void streamingUpload_noProfile_isForbiddenAndNothingIsStreamed() throws Exception {
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.empty());
    Source<ByteString, ?> source = createMultipartRequestBody("hello.png", PNG_HEADER);

    F.Either<Result, Http.MultipartFormData<String>> either = parse(source);

    assertThat(either.left).isPresent();
    assertThat(either.left.get().status()).isEqualTo(Http.Status.FORBIDDEN);
    verify(sinks, never()).getSinkForCloudProvider(any(BucketType.class), anyString(), anyInt());
  }

  @Test
  public void streamingUpload_nonAdminProfile_isForbiddenAndNothingIsStreamed() throws Exception {
    when(profile.isCiviFormAdmin()).thenReturn(false);
    Source<ByteString, ?> source = createMultipartRequestBody("hello.png", PNG_HEADER);

    F.Either<Result, Http.MultipartFormData<String>> either = parse(source);

    assertThat(either.left).isPresent();
    assertThat(either.left.get().status()).isEqualTo(Http.Status.FORBIDDEN);
    verify(sinks, never()).getSinkForCloudProvider(any(BucketType.class), anyString(), anyInt());
  }

  private F.Either<Result, Http.MultipartFormData<String>> parse(Source<ByteString, ?> source) {
    Http.RequestHeader request =
        fakeRequest()
            .method("POST")
            .uri(String.format("/admin/questions/%d/image/upload", QUESTION_ID))
            .header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY)
            .build();

    CompletionStage<F.Either<Result, Http.MultipartFormData<String>>> stage =
        parser.apply(request).run(source, materializer);
    return stage.toCompletableFuture().join();
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
