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
 * Question image upload implementation of the streaming multipart parser.
 *
 * <p>Streams a question image file upload to the public applicant bucket. The cloud-storage file
 * key is generated server-side using {@link services.cloud.PublicFileNameFormatter}.
 */
public class QuestionImageStreamingMultipartBodyParser extends StreamingMultipartBodyParser {
  public static final long MAX_FILE_SIZE = 1L * 1024L * 1024L; // 1MB

  // Matches /admin/questions/questionId/image/upload in the request path.
  private static final Pattern QUESTION_IMAGE_UPLOAD_PATH_PATTERN =
      Pattern.compile("/admin/questions/(\\d+)/image/upload(/|$)");

  private final ProfileUtils profileUtils;

  private long questionId;

  @Inject
  public QuestionImageStreamingMultipartBodyParser(
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
    Matcher matcher = QUESTION_IMAGE_UPLOAD_PATH_PATTERN.matcher(request.path());
    if (!matcher.find()) {
      throw new IllegalStateException(
          "Request path does not match expected question image upload pattern: " + request.path());
    }
    this.questionId = Long.parseLong(matcher.group(1));
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
    return PublicFileNameFormatter.formatPublicQuestionImageFileKey(
        questionId, fileInfo.fileName());
  }

  @Override
  protected ImmutableList<FileTypeSpecifier> getAllowedFileTypeSpecifiers() {
    return ImmutableList.of(FileTypeSpecifier.JPG, FileTypeSpecifier.JPEG, FileTypeSpecifier.PNG);
  }
}
