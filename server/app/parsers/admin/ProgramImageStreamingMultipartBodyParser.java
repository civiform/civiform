package parsers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import java.util.concurrent.CompletableFuture;
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
import services.cloud.BucketType;
import services.cloud.PublicFileNameFormatter;

/**
 * Program image upload implementation of the streaming multipart parser.
 *
 * <p>Streams a program image file upload to the public applicant bucket. The cloud-storage file key
 * is generated server-side using {@link services.cloud.PublicFileNameFormatter}.
 */
public final class ProgramImageStreamingMultipartBodyParser extends StreamingMultipartBodyParser {

  public static final long MAX_FILE_SIZE = 1L * 1024L * 1024L; // 1MB

  // Matches /admin/programs/{programId}/image/upload/{editStatus} in the request path.
  private static final Pattern PROGRAM_IMAGE_UPLOAD_PATH_PATTERN =
      Pattern.compile("/admin/programs/(\\d+)/image/upload/([^/]+)(/|$)");

  private final ProfileUtils profileUtils;

  private long programId;

  @Inject
  public ProgramImageStreamingMultipartBodyParser(
      Materializer materializer,
      DefaultHttpErrorHandler errorHandler,
      MultipartUploadSinks streamingMultipartUploadSinks,
      FileTypeValidation fileTypeValidation,
      ProfileUtils profileUtils) {
    super(
        materializer,
        errorHandler,
        streamingMultipartUploadSinks,
        fileTypeValidation,
        MAX_FILE_SIZE);
    this.profileUtils = checkNotNull(profileUtils);
  }

  @Override
  protected void parseRequestPath(Http.RequestHeader request) {
    Matcher matcher = PROGRAM_IMAGE_UPLOAD_PATH_PATTERN.matcher(request.path());
    if (!matcher.find()) {
      throw new IllegalStateException(
          "Request path does not contain program id: " + request.path());
    }
    this.programId = Long.parseLong(matcher.group(1));
  }

  /** Mirrors the {@code @Secure(CIVIFORM_ADMIN)} check on the upload action. */
  @Override
  protected CompletionStage<Boolean> isAuthorized(Http.RequestHeader request) {
    boolean isCiviFormAdmin =
        profileUtils
            .optionalCurrentUserProfile(request)
            .map(profile -> profile.isCiviFormAdmin())
            .orElse(false);

    return CompletableFuture.completedFuture(isCiviFormAdmin);
  }

  @Override
  protected BucketType getBucketType() {
    return BucketType.PUBLIC_BUCKET;
  }

  @Override
  protected String getFileKey(Multipart.FileInfo fileInfo) {
    return PublicFileNameFormatter.formatPublicProgramImageFileKey(programId, fileInfo.fileName());
  }

  @Override
  protected ImmutableList<FileTypeSpecifier> getAllowedFileTypeSpecifiers() {
    return ImmutableList.of(FileTypeSpecifier.JPG, FileTypeSpecifier.JPEG, FileTypeSpecifier.PNG);
  }
}
