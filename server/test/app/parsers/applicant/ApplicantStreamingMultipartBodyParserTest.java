package parsers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
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
import play.mvc.Http;
import repository.ResetPostgres;
import services.cloud.BucketType;
import services.cloud.StorageServiceName;
import services.settings.SettingsManifest;

public class ApplicantStreamingMultipartBodyParserTest extends ResetPostgres {
  private static final String MULTIPART_BOUNDARY = "boundary";
  private static final long APPLICANT_ID = 42L;
  private static final long PROGRAM_ID = 7L;
  private static final String BLOCK_ID = "3";
  // Valid PDF header bytes (%PDF-1.4 + comment line), at least 16 bytes for FileTypeValidation
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

  private ApplicantStreamingMultipartBodyParser parser;
  private Materializer materializer;
  private ProfileUtils profileUtils;

  @Before
  public void setUp() {
    materializer = instanceOf(Materializer.class);
    DefaultHttpErrorHandler errorHandler = instanceOf(DefaultHttpErrorHandler.class);

    MultipartUploadSinks sinks = mock(MultipartUploadSinks.class);
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
    CiviFormProfile profile = mock(CiviFormProfile.class);
    CiviFormProfileData profileData = mock(CiviFormProfileData.class);
    when(profile.getProfileData()).thenReturn(profileData);
    when(profileData.getAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, Long.class))
        .thenReturn(APPLICANT_ID);
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(profile));

    parser =
        new ApplicantStreamingMultipartBodyParser(
            materializer,
            errorHandler,
            sinks,
            instanceOf(FileTypeValidation.class),
            instanceOf(SettingsManifest.class),
            profileUtils);
  }

  @Test
  public void streamingUpload_withApplicantIdInPath_producesFileKeyWithUuidAndApplicantPrefix()
      throws Exception {
    Http.RequestHeader request =
        fakeRequest()
            .method("POST")
            .uri(
                String.format(
                    "/applicants/%d/programs/%d/blocks/%s/hx/selectFileForUpload",
                    APPLICANT_ID, PROGRAM_ID, BLOCK_ID))
            .header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY)
            .build();

    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.empty());

    Source<ByteString, ?> source = createMultipartRequestBody("hello.pdf", PDF_HEADER);

    CompletionStage<play.libs.F.Either<play.mvc.Result, Http.MultipartFormData<String>>> stage =
        parser.apply(request).run(source, materializer);
    Http.MultipartFormData<String> body = stage.toCompletableFuture().join().right.get();

    Http.MultipartFormData.FilePart<String> filePart = body.getFile("file");
    assertThat(filePart).isNotNull();
    assertThat(filePart.getRef())
        .startsWith(
            String.format("applicant-%d/program-%d/block-%s/", APPLICANT_ID, PROGRAM_ID, BLOCK_ID));
  }

  @Test
  public void streamingUpload_producesFileKeyWithUuidAndApplicantPrefix() throws Exception {
    Http.RequestHeader request =
        fakeRequest()
            .method("POST")
            .uri(
                String.format(
                    "/programs/%d/blocks/%s/hx/selectFileForUpload", PROGRAM_ID, BLOCK_ID))
            .header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY)
            .build();

    Source<ByteString, ?> source = createMultipartRequestBody("hello.pdf", PDF_HEADER);

    CompletionStage<play.libs.F.Either<play.mvc.Result, Http.MultipartFormData<String>>> stage =
        parser.apply(request).run(source, materializer);
    Http.MultipartFormData<String> body = stage.toCompletableFuture().join().right.get();

    Http.MultipartFormData.FilePart<String> filePart = body.getFile("file");
    assertThat(filePart).isNotNull();
    assertThat(filePart.getFilename()).isEqualTo("hello.pdf");

    String fileKey = filePart.getRef();
    assertThat(fileKey)
        .startsWith(
            String.format("applicant-%d/program-%d/block-%s/", APPLICANT_ID, PROGRAM_ID, BLOCK_ID));
    assertThat(fileKey).endsWith(".pdf");
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
                + "Content-Type: application/pdf\r\n\r\n");
    ByteString footer = ByteString.fromString("\r\n--" + MULTIPART_BOUNDARY + "--\r\n");
    return Source.single(header.concat(ByteString.fromArray(content)).concat(footer));
  }
}
