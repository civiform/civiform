package parsers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.ProfileUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
import services.cloud.StorageServiceName;
import services.settings.SettingsManifest;

public class ApplicantStreamingMultipartBodyParserTest extends ResetPostgres {
  private static final String MULTIPART_BOUNDARY = "boundary";
  private static final long APPLICANT_ID = 42L;
  private static final long PROGRAM_ID = 7L;
  private static final String BLOCK_ID = "3";
  private static final String APPLICANT_PATH_URI =
      String.format(
          "/applicants/%d/programs/%d/blocks/%s/hx/selectFileForUpload",
          APPLICANT_ID, PROGRAM_ID, BLOCK_ID);
  private static final String GUEST_URI =
      String.format("/programs/%d/blocks/%s/hx/selectFileForUpload", PROGRAM_ID, BLOCK_ID);
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
  private MultipartUploadSinks sinks;
  private ProfileUtils profileUtils;
  private CiviFormProfile profile;
  private CiviFormProfileData profileData;

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

    // Default profile: an applicant who owns APPLICANT_ID and is neither a TI nor an admin.
    profileUtils = mock(ProfileUtils.class);
    profile = mock(CiviFormProfile.class);
    profileData = mock(CiviFormProfileData.class);
    when(profile.getProfileData()).thenReturn(profileData);
    when(profileData.getAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, Long.class))
        .thenReturn(APPLICANT_ID);
    when(profile.checkAuthorization(APPLICANT_ID))
        .thenReturn(CompletableFuture.completedFuture(null));
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
  public void streamingUpload_withApplicantIdInPath_asTi_producesFileKeyWithApplicantPrefix()
      throws Exception {
    when(profile.isTrustedIntermediary()).thenReturn(true);
    Source<ByteString, ?> source = createMultipartRequestBody("hello.pdf", PDF_HEADER);

    Http.MultipartFormData<String> body = parse(APPLICANT_PATH_URI, source).right.get();

    Http.MultipartFormData.FilePart<String> filePart = body.getFile("file");
    assertThat(filePart).isNotNull();
    assertThat(filePart.getRef())
        .startsWith(
            String.format("applicant-%d/program-%d/block-%s/", APPLICANT_ID, PROGRAM_ID, BLOCK_ID));
  }

  @Test
  public void streamingUpload_withApplicantIdInPath_asAdmin_isAllowed() throws Exception {
    when(profile.isCiviFormAdmin()).thenReturn(true);
    Source<ByteString, ?> source = createMultipartRequestBody("hello.pdf", PDF_HEADER);

    F.Either<Result, Http.MultipartFormData<String>> either = parse(APPLICANT_PATH_URI, source);

    assertThat(either.right).isPresent();
  }

  @Test
  public void streamingUpload_withApplicantIdInPath_noProfile_isForbiddenAndNothingIsStreamed()
      throws Exception {
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.empty());
    Source<ByteString, ?> source = createMultipartRequestBody("hello.pdf", PDF_HEADER);

    F.Either<Result, Http.MultipartFormData<String>> either = parse(APPLICANT_PATH_URI, source);

    assertThat(either.left).isPresent();
    assertThat(either.left.get().status()).isEqualTo(Http.Status.FORBIDDEN);
    verify(sinks, never()).getSinkForCloudProvider(any(BucketType.class), anyString(), anyInt());
  }

  @Test
  public void streamingUpload_withApplicantIdInPath_asPlainApplicant_isForbidden()
      throws Exception {
    // The applicant owns the id, but the path route is reserved for TIs and admins.
    Source<ByteString, ?> source = createMultipartRequestBody("hello.pdf", PDF_HEADER);

    F.Either<Result, Http.MultipartFormData<String>> either = parse(APPLICANT_PATH_URI, source);

    assertThat(either.left).isPresent();
    assertThat(either.left.get().status()).isEqualTo(Http.Status.FORBIDDEN);
    verify(sinks, never()).getSinkForCloudProvider(any(BucketType.class), anyString(), anyInt());
  }

  @Test
  public void streamingUpload_withApplicantIdInPath_tiNotAuthorizedForApplicant_isForbidden()
      throws Exception {
    when(profile.isTrustedIntermediary()).thenReturn(true);
    when(profile.checkAuthorization(APPLICANT_ID))
        .thenReturn(
            CompletableFuture.failedFuture(new SecurityException("not in the applicant's group")));
    Source<ByteString, ?> source = createMultipartRequestBody("hello.pdf", PDF_HEADER);

    F.Either<Result, Http.MultipartFormData<String>> either = parse(APPLICANT_PATH_URI, source);

    assertThat(either.left).isPresent();
    assertThat(either.left.get().status()).isEqualTo(Http.Status.FORBIDDEN);
    verify(sinks, never()).getSinkForCloudProvider(any(BucketType.class), anyString(), anyInt());
  }

  @Test
  public void streamingUpload_producesFileKeyWithUuidAndApplicantPrefix() throws Exception {
    Source<ByteString, ?> source = createMultipartRequestBody("hello.pdf", PDF_HEADER);

    Http.MultipartFormData<String> body = parse(GUEST_URI, source).right.get();

    Http.MultipartFormData.FilePart<String> filePart = body.getFile("file");
    assertThat(filePart).isNotNull();
    assertThat(filePart.getFilename()).isEqualTo("hello.pdf");

    String fileKey = filePart.getRef();
    assertThat(fileKey)
        .startsWith(
            String.format("applicant-%d/program-%d/block-%s/", APPLICANT_ID, PROGRAM_ID, BLOCK_ID));
    assertThat(fileKey).endsWith(".pdf");
  }

  @Test
  public void streamingUpload_noProfile_isForbiddenAndNothingIsStreamed() throws Exception {
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.empty());
    Source<ByteString, ?> source = createMultipartRequestBody("hello.pdf", PDF_HEADER);

    F.Either<Result, Http.MultipartFormData<String>> either = parse(GUEST_URI, source);

    assertThat(either.left).isPresent();
    assertThat(either.left.get().status()).isEqualTo(Http.Status.FORBIDDEN);
    verify(sinks, never()).getSinkForCloudProvider(any(BucketType.class), anyString(), anyInt());
  }

  @Test
  public void streamingUpload_profileWithoutApplicantId_isForbiddenAndNothingIsStreamed()
      throws Exception {
    when(profileData.getAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, Long.class))
        .thenReturn(null);
    Source<ByteString, ?> source = createMultipartRequestBody("hello.pdf", PDF_HEADER);

    F.Either<Result, Http.MultipartFormData<String>> either = parse(GUEST_URI, source);

    assertThat(either.left).isPresent();
    assertThat(either.left.get().status()).isEqualTo(Http.Status.FORBIDDEN);
    verify(sinks, never()).getSinkForCloudProvider(any(BucketType.class), anyString(), anyInt());
  }

  private F.Either<Result, Http.MultipartFormData<String>> parse(
      String uri, Source<ByteString, ?> source) {
    Http.RequestHeader request =
        fakeRequest()
            .method("POST")
            .uri(uri)
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
                + "Content-Type: application/pdf\r\n\r\n");
    ByteString footer = ByteString.fromString("\r\n--" + MULTIPART_BOUNDARY + "--\r\n");
    return Source.single(header.concat(ByteString.fromArray(content)).concat(footer));
  }
}
