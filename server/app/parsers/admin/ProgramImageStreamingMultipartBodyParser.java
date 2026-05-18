package parsers.admin;

import com.google.common.collect.ImmutableList;
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
import services.cloud.PublicFileNameFormatter;
import services.cloud.BucketType;

import javax.inject.Inject;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Admin file upload implementation of the streaming multipart parser.
 *
 * <p>Streams an admin file upload to the public s3 bucket. The cloud-storage file key
 * is generated server-side using {@link PublicFileNameFormatter}.
 */
public final class ProgramImageStreamingMultipartBodyParser extends StreamingMultipartBodyParser {

  public static final long MAX_FILE_SIZE = 1024 * 1024; // 1MB

  private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png");

  // Matches "program-summary-image/program-";{programId}/* in the request path.
  private static final Pattern PROGRAM_IMAGE_PATH_PATTERN =
    Pattern.compile("/programs-summary-image/program-(\\d+)/(/|$)");

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
      MAX_FILE_SIZE);
  }

  @Override
  protected ImmutableList<FileTypeSpecifier> getAllowedFileTypeSpecifiers() {
    return FileTypeSpecifier.parseCommaSeparated(ALLOWED_MIME_TYPES.toString());
  }

  @Override
  public Accumulator<ByteString, F.Either<Result, Http.MultipartFormData<String>>> apply(
    Http.RequestHeader request) {
    Matcher matcher = PROGRAM_IMAGE_PATH_PATTERN.matcher(request.path());
    if (!matcher.find()) {
      throw new IllegalStateException(
        "Request path does not contain program image: " + request.path());
    }
    this.programId = Long.parseLong(matcher.group(1));
    return super.apply(request);
  }

  @Override
  protected BucketType getBucketType() {
    return BucketType.PUBLIC_BUCKET;
  }

  @Override
  protected String getFileKey(Multipart.FileInfo fileInfo) {
    checkNotNull(programId);
    return PublicFileNameFormatter.formatPublicProgramImageFileKey(programId).replace("${filename}", fileInfo.fileName());
  }
}
