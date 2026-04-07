package parsers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileFactory;
import auth.ProfileUtils;
import com.typesafe.config.Config;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.util.ByteString;
import parsers.StreamingMultipartBodyParser;
import parsers.cloud.MultipartUploadSinks;
import play.core.parsers.Multipart;
import play.http.DefaultHttpErrorHandler;
import play.libs.F;
import play.libs.streams.Accumulator;
import play.mvc.Http;
import play.mvc.Result;
import services.cloud.ApplicantFileNameFormatter;

/**
 * Applicant file upload implementation of the streaming multipart parser.
 *
 * <p>Streams an applicant file upload to the private applicant bucket. The cloud-storage file key
 * is generated server-side using {@link ApplicantFileNameFormatter}.
 */
public final class ApplicantStreamingMultipartBodyParser extends StreamingMultipartBodyParser {

  public static final long MAX_FILE_SIZE = 100L * 1024L * 1024L; // 100MB

  // Matches /programs/{programId}/blocks/{blockId}/* in the request path.
  private static final Pattern PROGRAM_BLOCK_PATH_PATTERN =
      Pattern.compile("/programs/(\\d+)/blocks/([^/]+)(/|$)");

  private final ProfileUtils profileUtils;
  private final String bucketName;

  private long applicantId;
  private long programId;
  private String blockId;

  @Inject
  public ApplicantStreamingMultipartBodyParser(
      Materializer materializer,
      DefaultHttpErrorHandler errorHandler,
      MultipartUploadSinks streamingMultipartUploadSinks,
      Config config,
      ProfileUtils profileUtils) {
    super(materializer, errorHandler, streamingMultipartUploadSinks, MAX_FILE_SIZE);
    this.profileUtils = checkNotNull(profileUtils);
    this.bucketName = resolvePrivateApplicantBucket(checkNotNull(config));
  }

  @Override
  public Accumulator<ByteString, F.Either<Result, Http.MultipartFormData<String>>> apply(
      Http.RequestHeader request) {
    Matcher matcher = PROGRAM_BLOCK_PATH_PATTERN.matcher(request.path());
    if (!matcher.find()) {
      throw new IllegalStateException(
          "Request path does not contain program or block ids: " + request.path());
    }
    this.programId = Long.parseLong(matcher.group(1));
    this.blockId = matcher.group(2);
    this.applicantId =
        profileUtils
            .optionalCurrentUserProfile(request)
            .map(
                profile ->
                    profile
                        .getProfileData()
                        .getAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, Long.class))
            .filter(Objects::nonNull)
            .orElseThrow(() -> new IllegalStateException("No applicant id on request"));
    return super.apply(request);
  }

  @Override
  protected String getBucketName() {
    return bucketName;
  }

  @Override
  protected String getFileKey(Multipart.FileInfo fileInfo) {
    return ApplicantFileNameFormatter.formatFileUploadQuestionFilenameWithUuid(
        applicantId, programId, blockId, fileInfo.fileName());
  }

  private static String resolvePrivateApplicantBucket(Config config) {
    String storageProvider = config.getString("cloud.storage");
    return switch (storageProvider) {
      case "gcp", "gcp-s3" -> config.getString("gcp.s3.bucket");
      default -> config.getString("aws.s3.bucket");
    };
  }
}
