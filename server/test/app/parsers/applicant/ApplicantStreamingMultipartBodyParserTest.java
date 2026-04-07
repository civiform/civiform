package parsers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.ProfileUtils;
import com.typesafe.config.Config;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.junit.Before;
import org.junit.Test;
import parsers.cloud.MultipartUploadSinks;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http;
import repository.ResetPostgres;

public class ApplicantStreamingMultipartBodyParserTest extends ResetPostgres {
  private static final String MULTIPART_BOUNDARY = "boundary";
  private static final long APPLICANT_ID = 42L;
  private static final long PROGRAM_ID = 7L;
  private static final String BLOCK_ID = "3";

  private ApplicantStreamingMultipartBodyParser parser;
  private Materializer materializer;
  private ProfileUtils profileUtils;

  @Before
  public void setUp() {
    materializer = instanceOf(Materializer.class);
    DefaultHttpErrorHandler errorHandler = instanceOf(DefaultHttpErrorHandler.class);
    MultipartUploadSinks sinks = instanceOf(MultipartUploadSinks.class);
    Config config = instanceOf(Config.class);

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
            materializer, errorHandler, sinks, config, profileUtils);
  }

  @Test
  public void getBucketName_resolvesFromConfig() {
    // The bucket is resolved at construction time from cloud.storage.
    assertThat(parser.getBucketName()).isNotEmpty();
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

    Source<ByteString, ?> source = createMultipartRequestBody("hello.pdf", "Hello world");

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

  private Source<ByteString, ?> createMultipartRequestBody(String filename, String content) {
    String requestBody =
        "--"
            + MULTIPART_BOUNDARY
            + "\r\n"
            + "Content-Disposition: form-data; name=\"file\"; filename=\""
            + filename
            + "\"\r\n"
            + "Content-Type: application/pdf\r\n\r\n"
            + content
            + "\r\n"
            + "--"
            + MULTIPART_BOUNDARY
            + "--\r\n";
    return Source.single(ByteString.fromString(requestBody));
  }
}
