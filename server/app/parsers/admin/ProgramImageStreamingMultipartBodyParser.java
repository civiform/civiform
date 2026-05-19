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
 * Program image upload implementation of the streaming multipart parser.
 *
 * <p>Streams a program image file upload to the public applicant bucket. The cloud-storage file key
 * is generated server-side using {@link services.cloud.PublicFileNameFormatter}.
 */
public final class ProgramImageStreamingMultipartBodyParser extends StreamingMultipartBodyParser {

  private static final BucketType BUCKET_TYPE = BucketType.PUBLIC_BUCKET;

  // Matches /admin/programs/{programId}/image/upload/{editStatus} in the request path.
  private static final Pattern PROGRAM_IMAGE_UPLOAD_PATH_PATTERN =
      Pattern.compile("/admin/programs/(\\d+)/image/upload/([^/]+)(/|$)");

  private long programId;

  @Inject
  public ProgramImageStreamingMultipartBodyParser(
      Materializer materializer,
      DefaultHttpErrorHandler errorHandler,
      MultipartUploadSinks streamingMultipartUploadSinks,
      FileTypeValidation fileTypeValidation) {
    super(
        materializer,
        errorHandler,
        streamingMultipartUploadSinks,
        fileTypeValidation,
        streamingMultipartUploadSinks.getMaxUploadSizeBytes(BUCKET_TYPE));
  }

  @Override
  public Accumulator<ByteString, F.Either<Result, Http.MultipartFormData<String>>> apply(
      Http.RequestHeader request) {
    Matcher matcher = PROGRAM_IMAGE_UPLOAD_PATH_PATTERN.matcher(request.path());
    if (!matcher.find()) {
      throw new IllegalStateException(
          "Request path does not contain program id: " + request.path());
    }
    this.programId = Long.parseLong(matcher.group(1));
    return super.apply(request);
  }

  @Override
  protected BucketType getBucketType() {
    return BUCKET_TYPE;
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
