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

  private record SniffState(ByteString bufferedHeader, boolean validated) {
    static final SniffState INITIAL = new SniffState(ByteString.emptyByteString(), false);
    static final SniffState VALIDATED = new SniffState(ByteString.emptyByteString(), true);
  }

  /**
   * Returns a pass-through Pekko {@link Flow} that sniffs the first {@link #HEADER_SIZE} bytes of
   * an upload stream, validates them against the filename's extension and the allowed upload types,
   * and reports the Tika-detected MIME type via {@code detectedMimeTypeRef}.
   *
   * <p>An upload pipeline in Pekko Streams is composed of a {@code Source} (the request body), zero
   * or more {@code Flow}s (transformations), and a {@code Sink} (the cloud storage upload). Bytes
   * are pulled through the graph in chunks. This {@code Flow} is inserted between the source and
   * the upload sink so that every chunk of bytes flows through our {@code .statefulMap(...)} on its
   * way to storage, where we inspect the prefix without buffering the entire file.
   *
   * <p>How this works:
   *
   * <ul>
   *   <li>{@link Flow#of(Class)} creates an identity {@code Flow<ByteString, ByteString>}, i.e. a
   *       stream whose input and output element type is {@link ByteString}.
   *   <li>{@code .statefulMap} tracks {@link SniffState}, buffering bytes until at least {@link
   *       #HEADER_SIZE} bytes have arrived and emitting empty chunks (dropped by {@code .filter})
   *       in the meantime so no bytes reach the downstream cloud storage upload sink before Tika
   *       validation succeeds. Once {@link #HEADER_SIZE} bytes are available, it calls {@link
   *       #validateHeaderBytes} to run Tika and throws {@link FileUploadTypeException} on any
   *       mismatch. On success, it records the detected MIME type in {@code detectedMimeTypeRef},
   *       flushes the buffered bytes downstream, and transitions to {@link SniffState#VALIDATED} so
   *       subsequent chunks pass straight through.
   *   <li>The end-of-stream callback fires when upstream finishes, including during failure cleanup
   *       triggered by a downstream throw, and throws {@link FileUploadTypeException} only when
   *       {@code !finalState.validated()} (fewer than {@link #HEADER_SIZE} bytes arrived). Any
   *       earlier validation failure or downstream failure propagates unmasked.
   *   <li>Throwing inside {@code .statefulMap} fails the stream under Pekko's default supervision
   *       strategy ("stop"), which propagates as an upstream failure to the downstream sink so the
   *       rejected file is never committed to cloud storage.
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
            () -> SniffState.INITIAL,
            (state, bytes) -> {
              if (state.validated()) {
                return Pair.create(state, bytes);
              }
              ByteString combined = state.bufferedHeader().concat(bytes);
              if (combined.size() < FileTypeValidation.HEADER_SIZE) {
                return Pair.create(new SniffState(combined, false), ByteString.emptyByteString());
              }
              String detectedMimeType =
                  validateHeaderBytes(
                      combined.take(FileTypeValidation.HEADER_SIZE), fileName, allowedFileTypes);
              detectedMimeTypeRef.set(detectedMimeType);
              return Pair.create(SniffState.VALIDATED, combined);
            },
            finalState -> {
              if (!finalState.validated()) {
                throw new FileUploadTypeException("File is too small to validate its type.");
              }
              return Optional.empty();
            })
        .filter(bytes -> !bytes.isEmpty());
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

  /**
   * Determines the MIME type for a given file name or path using Apache Tika, falling back to
   * {@link FileTypeSpecifier#MIME_BY_EXTENSION_MAP} if Tika returns octet-stream.
   */
  public static Optional<String> determineMimeType(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return Optional.empty();
    }
    String detectedMime = TIKA.detect(fileName);
    if (detectedMime != null
        && !detectedMime.isEmpty()
        && !"application/octet-stream".equals(detectedMime)) {
      return Optional.of(detectedMime);
    }
    String extension = FilenameUtils.getExtension(fileName);
    return FileTypeSpecifier.getMimeTypeForExtension(extension);
  }
}
