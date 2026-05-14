package parsers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.util.ByteString;
import org.apache.tika.Tika;

/**
 * Validates uploaded file content by looking up the MIME type for the filename's extension,
 * checking it against the allowed upload types, and confirming with Apache Tika that the file's
 * bytes actually match that type. Throws {@link FileUploadTypeException} on any mismatch.
 */
public final class FileTypeValidation {
  private static final Tika TIKA = new Tika();

  private static final int HEADER_SIZE = 16;

  private static final ImmutableMap<String, String> MIME_BY_EXTENSION =
      FileTypeSpecifier.MIME_BY_EXTENSION_MAP;

  @Inject
  public FileTypeValidation() {}

  /** Parses a comma-separated list of MIME types, wildcards, and extensions. */
  @VisibleForTesting
  static ImmutableList<String> parseSpecifiers(String specifiers) {
    return Splitter.on(',')
        .trimResults()
        .omitEmptyStrings()
        .splitToStream(specifiers)
        .map(spec -> spec.toLowerCase(Locale.ROOT))
        .flatMap(spec -> Stream.ofNullable(normalizeSpecifier(spec)))
        .collect(ImmutableList.toImmutableList());
  }

  private static String normalizeSpecifier(String normalized) {
    if (normalized.endsWith("/*")) {
      return normalized;
    }
    if (normalized.startsWith(".")) {
      return MIME_BY_EXTENSION.get(normalized);
    }
    return normalized;
  }

  private static boolean isAllowedType(String mimeType, ImmutableList<String> allowedTypes) {
    return allowedTypes.stream()
        .anyMatch(
            allowed ->
                allowed.endsWith("/*")
                    ? mimeType.startsWith(allowed.substring(0, allowed.length() - 1))
                    : mimeType.equals(allowed));
  }

  /**
   * Returns a pass-through Pekko {@link Flow} that sniffs the first {@link #HEADER_SIZE} bytes of
   * an upload stream, validates them against the filename's extension and the allowed upload types,
   * and reports the Tika-detected MIME type via {@code detectedMimeTypeRef}.
   *
   * <p>An upload pipeline in Pekko Streams is composed of a {@code Source} (the request body), zero
   * or more {@code Flow}s (transformations), and a {@code Sink} (the cloud storage upload). Bytes
   * are pulled through the graph in chunks. This {@code Flow} is inserted between the source and
   * the upload sink so that every chunk of bytes flows through our {@code .stateMap(...)} and
   * {@code .map(...)} on its way to storage, where we inspect the prefix without buffering the
   * entire file.
   *
   * <p>How this works:
   *
   * <ul>
   *   <li>{@link Flow#of(Class)} creates an identity {@code Flow<ByteString, ByteString>}, i.e. a
   *       stream whose input and output element type is {@link ByteString}.
   *   <li>{@code .statefulMap} accumulates the first {@link #HEADER_SIZE} bytes into the stage's
   *       state. Each element is emitted as a {@link Pair} of {@code (currentHeader, bytes)}. The
   *       end-of-stream callback fires when upstream finishes, including during failure cleanup
   *       triggered by a downstream throw, and throws {@link FileUploadTypeException} only when the
   *       final state is shorter than {@link #HEADER_SIZE}. Any earlier validation failure from the
   *       {@code .map} propagates unmasked.
   *   <li>When the accumulated header first reaches {@link #HEADER_SIZE}, the map hands it to
   *       {@link #validateHeaderBytes}, which runs Tika and throws {@link FileUploadTypeException}
   *       on any mismatch. {@code detectedMimeTypeRef} is only populated if the bytes have already
   *       been validated, so we only call Tika once.
   *   <li>Throwing inside the {@code .map} function fails the stream under Pekko's default
   *       supervision strategy ("stop"), which propagates as an upstream failure to the downstream
   *       sink. The S3 / GCS Pekko connectors abort the in-progress multipart upload on upstream
   *       failure, so the rejected file is not committed to cloud storage.
   *   <li>Throwing inside the cleanup function also aborts the upload. The cleanup still runs after
   *       a downstream failure or completion in the {@code .map}.
   *   <li>The detected MIME type is written into {@code detectedMimeTypeRef} so the caller can
   *       attach the MIME to the resulting {@code FilePart}.
   *   <li>{@code allowedFileTypeSpecifiers} is converted to normalized MIME / wildcard entries
   *       before validation runs.
   * </ul>
   *
   * <p>Pekko Streams references:
   *
   * <ul>
   *   <li><a
   *       href="https://pekko.apache.org/docs/pekko/current/stream/stream-flows-and-basics.html">Streams
   *       and flows overview</a>
   *   <li><a
   *       href="https://pekko.apache.org/docs/pekko/current/stream/operators/Source-or-Flow/statefulMap.html">{@code
   *       statefulMap} operator</a>
   *   <li><a href="https://pekko.apache.org/docs/pekko/current/stream/stream-error.html">Error
   *       handling and supervision</a>
   * </ul>
   */
  public Flow<ByteString, ByteString, ?> sniffingFlow(
      String fileName,
      AtomicReference<String> detectedMimeTypeRef,
      ImmutableList<FileTypeSpecifier> allowedFileTypeSpecifiers) {
    if (allowedFileTypeSpecifiers.isEmpty()) {
      throw new IllegalArgumentException("At least one FileTypeSpecifier is required");
    }
    ImmutableList<String> allowedFileTypes =
        allowedFileTypeSpecifiers.stream()
            .map(FileTypeSpecifier::normalizedAllowEntry)
            .collect(ImmutableList.toImmutableList());
    return Flow.of(ByteString.class)
        .statefulMap(
            ByteString::emptyByteString,
            (accumulatedHeader, bytes) -> {
              ByteString currentHeader = accumulatedHeader;
              if (accumulatedHeader.size() < FileTypeValidation.HEADER_SIZE) {
                int remainingBytes = FileTypeValidation.HEADER_SIZE - accumulatedHeader.size();
                currentHeader = accumulatedHeader.concat(bytes.take(remainingBytes));
              }
              return Pair.create(currentHeader, Pair.create(currentHeader, bytes));
            },
            finalHeader -> {
              if (finalHeader.size() < FileTypeValidation.HEADER_SIZE) {
                throw new FileUploadTypeException("File is too small to validate its type.");
              }
              return Optional.empty();
            })
        .map(
            finalHeaderAndBytes -> {
              ByteString finalHeader = finalHeaderAndBytes.first();
              ByteString bytes = finalHeaderAndBytes.second();
              if (finalHeader.size() >= FileTypeValidation.HEADER_SIZE
                  && detectedMimeTypeRef.get() == null) {
                String detectedMimeType =
                    validateHeaderBytes(finalHeader, fileName, allowedFileTypes);
                detectedMimeTypeRef.set(detectedMimeType);
              }
              return bytes;
            });
  }

  @VisibleForTesting
  String validateHeaderBytes(
      ByteString headerBytes, String fileName, ImmutableList<String> allowedTypes) {

    String extension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);
    String expectedMimeType = extension.isEmpty() ? null : MIME_BY_EXTENSION.get("." + extension);
    if (expectedMimeType == null) {
      throw new FileUploadTypeException(
          String.format("Extension .%s is not a supported upload type.", extension));
    }

    if (!isAllowedType(expectedMimeType, allowedTypes)) {
      throw new FileUploadTypeException(
          String.format("File type %s is not an allowed upload type.", expectedMimeType));
    }

    String detectedMimeType = TIKA.detect(headerBytes.toArray());
    if (!expectedMimeType.equals(detectedMimeType)) {
      throw new FileUploadTypeException(
          String.format(
              "Content bytes do not match extension .%s (detected %s).",
              extension, detectedMimeType));
    }
    return detectedMimeType;
  }
}
