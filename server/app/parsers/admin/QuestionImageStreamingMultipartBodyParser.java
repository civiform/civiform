package parsers.admin;

import com.google.common.collect.ImmutableList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.util.ByteString;
import parsers.FileTypeSpecifier;
import parsers.FileTypeValidation;
import parsers.StreamingMultipartBodyParser;
import parsers.cloud.MultipartUploadSinks;
import play.core.parsers.Multipart;
import play.http.DefaultHttpErrorHandler;
import play.libs.F;
import play.libs.streams.Accumulator;
import play.mvc.Http;
import play.mvc.Result;
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

  private long questionId;

  @Inject
  public QuestionImageStreamingMultipartBodyParser(
      Materializer materializer,
      DefaultHttpErrorHandler errorHandler,
      MultipartUploadSinks streamingMultipartUploadSinks,
      FileTypeValidation fileTypeValidation) {
    super(
        materializer,
        errorHandler,
        streamingMultipartUploadSinks,
        fileTypeValidation,
        MAX_FILE_SIZE);
  }

  @Override
  public Accumulator<ByteString, F.Either<Result, Http.MultipartFormData<String>>> apply(
      Http.RequestHeader request) {
    Matcher matcher = QUESTION_IMAGE_UPLOAD_PATH_PATTERN.matcher(request.path());
    if (!matcher.find()) {
      throw new IllegalStateException(
          "Request path does not match expected question image upload pattern: " + request.path());
    }
    this.questionId = Long.parseLong(matcher.group(1));
    return super.apply(request);
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
