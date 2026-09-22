package parsers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileFactory;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.apache.pekko.stream.Materializer;
import parsers.FileTypeSpecifier;
import parsers.FileTypeValidation;
import parsers.StreamingMultipartBodyParser;
import parsers.cloud.MultipartUploadSinks;
import play.core.parsers.Multipart;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http;
import services.cloud.ApplicantFileNameFormatter;
import services.cloud.BucketType;
import services.settings.SettingsManifest;

/**
 * Applicant file upload implementation of the streaming multipart parser.
 *
 * <p>Streams an applicant file upload to the private applicant bucket. The cloud-storage file key
 * is generated server-side using {@link ApplicantFileNameFormatter}.
 */
public final class ApplicantStreamingMultipartBodyParser extends StreamingMultipartBodyParser {

  public static final long MAX_FILE_SIZE = 100L * 1024L * 1024L; // 100MB

  // Matches /programs/{programId}/blocks/{blockId}/* or
  // /applicants/{applicantId}/programs/{programId}/blocks/{blockId}/* in the request path.
  private static final Pattern PROGRAM_BLOCK_PATH_PATTERN =
      Pattern.compile("(?:/applicants/(\\d+))?/programs/(\\d+)/blocks/([^/]+)(/|$)");

  private final ProfileUtils profileUtils;
  private final SettingsManifest settingsManifest;

  private Optional<Long> applicantIdFromPath = Optional.empty();
  private long applicantId;
  private long programId;
  private String blockId;

  @Inject
  public ApplicantStreamingMultipartBodyParser(
      Materializer materializer,
      DefaultHttpErrorHandler errorHandler,
      MultipartUploadSinks streamingMultipartUploadSinks,
      FileTypeValidation fileTypeValidation,
      SettingsManifest settingsManifest,
      ProfileUtils profileUtils) {
    super(
        materializer,
        errorHandler,
        streamingMultipartUploadSinks,
        fileTypeValidation,
        MAX_FILE_SIZE);
    this.profileUtils = checkNotNull(profileUtils);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  @Override
  protected void parseRequestPath(Http.RequestHeader request) {
    Matcher matcher = PROGRAM_BLOCK_PATH_PATTERN.matcher(request.path());
    if (!matcher.find()) {
      throw new IllegalStateException(
          "Request path does not contain program or block ids: " + request.path());
    }

    this.applicantIdFromPath = Optional.ofNullable(matcher.group(1)).map(Long::parseLong);
    this.programId = Long.parseLong(matcher.group(2));
    this.blockId = matcher.group(3);
  }

  /**
   * Mirrors the checks on the upload actions. The route with an applicant id in the path is
   * restricted to TIs and CiviForm admins who are authorized for that applicant. The route without
   * one requires a profile that carries an applicant id. This also resolves the applicant id used
   * to build the file key.
   */
  @Override
  protected CompletionStage<Boolean> isAuthorized(Http.RequestHeader request) {
    Optional<CiviFormProfile> maybeProfile = profileUtils.optionalCurrentUserProfile(request);
    if (maybeProfile.isEmpty()) {
      return CompletableFuture.completedFuture(false);
    }
    CiviFormProfile profile = maybeProfile.get();

    if (applicantIdFromPath.isPresent()) {
      if (!profile.isTrustedIntermediary() && !profile.isCiviFormAdmin()) {
        return CompletableFuture.completedFuture(false);
      }
      this.applicantId = applicantIdFromPath.get();

      return profile
          .checkAuthorization(applicantId)
          .thenApply(unused -> true)
          .exceptionally(
              error -> {
                if (isSecurityException(error)) {
                  return false;
                }
                throw new CompletionException(error);
              });
    }

    Long applicantIdFromProfile =
        profile
            .getProfileData()
            .getAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, Long.class);
    if (applicantIdFromProfile == null) {
      return CompletableFuture.completedFuture(false);
    }
    this.applicantId = applicantIdFromProfile;

    return CompletableFuture.completedFuture(true);
  }

  private static boolean isSecurityException(Throwable error) {
    Throwable current = error;
    while (current != null) {
      if (current instanceof SecurityException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  @Override
  protected BucketType getBucketType() {
    return BucketType.PRIVATE_BUCKET;
  }

  @Override
  protected String getFileKey(Multipart.FileInfo fileInfo) {
    return ApplicantFileNameFormatter.formatFileUploadQuestionFilenameWithUuid(
        applicantId, programId, blockId, fileInfo.fileName());
  }

  @Override
  protected ImmutableList<FileTypeSpecifier> getAllowedFileTypeSpecifiers() {
    return FileTypeSpecifier.parseCommaSeparated(
        settingsManifest.getFileUploadAllowedFileTypeSpecifiers().get());
  }
}
