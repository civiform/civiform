package parsers;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.pekko.util.ByteString;

/**
 * Validates uploaded file content by comparing magic bytes (file header signatures) against the
 * declared content type. Throws {@link FileUploadTypeException} when the actual file type does not
 * match what was declared, or when the detected type is not an allowed upload type.
 */
public final class FileTypeValidation {

  @Inject
  public FileTypeValidation() {}

  static final int HEADER_SIZE = 16;

  private static final ImmutableList<FileType> FILE_TYPES =
      ImmutableList.of(
          FileType.of(
              ImmutableList.of(".png"),
              "image/png",
              new byte[][] {{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}}),
          FileType.of(
              ImmutableList.of(".jpg", ".jpeg"),
              "image/jpeg",
              new byte[][] {{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}}),
          FileType.of(
              ImmutableList.of(".gif"),
              "image/gif",
              new byte[][] {
                {(byte) 0x47, 0x49, 0x46, 0x38, 0x37, 0x61},
                {(byte) 0x47, 0x49, 0x46, 0x38, 0x39, 0x61}
              }),
          FileType.of(
              ImmutableList.of(".pdf"),
              "application/pdf",
              new byte[][] {{0x25, 0x50, 0x44, 0x46, 0x2D}}),
          FileType.of(ImmutableList.of(".bmp"), "image/bmp", new byte[][] {{0x42, 0x4D}}),
          FileType.of(
              ImmutableList.of(".webp"),
              "image/webp",
              new byte[][] {{0x52, 0x49, 0x46, 0x46}}), // RIFF
          FileType.of(
              ImmutableList.of(".tiff"),
              "image/tiff",
              new byte[][] {
                {0x49, 0x49, 0x2A, 0x00},
                {0x4D, 0x4D, 0x00, 0x2A}
              }),
          FileType.of(
              ImmutableList.of(".zip"), "application/zip", new byte[][] {{0x50, 0x4B, 0x03, 0x04}}),
          FileType.of(
              ImmutableList.of(".xlsx"),
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              new byte[][] {{0x50, 0x4B, 0x03, 0x04}}));

  static {
    int maxSig =
        FILE_TYPES.stream()
            .flatMap(ft -> ft.magicSignatures().stream())
            .mapToInt(ByteString::size)
            .max()
            .orElse(0);
    if (maxSig > HEADER_SIZE) {
      throw new IllegalStateException(
          String.format(
              "FileTypeValidation: a magic signature (%d bytes) exceeds HEADER_SIZE (%d).",
              maxSig, HEADER_SIZE));
    }
  }

  private static final ImmutableMap<String, FileType> FILE_TYPE_BY_EXTENSION =
      FILE_TYPES.stream()
          .flatMap(
              ft ->
                  ft.extensions().stream().map(ext -> Map.entry(ext.toLowerCase(Locale.ROOT), ft)))
          .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

  public void validateHeaderBytes(
      ByteString headerBytes,
      String declaredContentType,
      String fileName,
      ImmutableList<String> allowedTypes) {

    String declaredNormalized =
        declaredContentType == null ? "" : declaredContentType.toLowerCase(Locale.ROOT).trim();

    if (!isAllowedType(declaredNormalized, allowedTypes)) {
      throw new FileUploadTypeException(
          String.format(
              "File \"%s\": declared content type \"%s\" is not an allowed upload type.",
              fileName, declaredContentType));
    }

    ImmutableList<FileType> matches = detectTypes(headerBytes);
    if (matches.isEmpty()) {
      throw new FileUploadTypeException(
          String.format("File \"%s\": could not verify file type from content bytes.", fileName));
    }

    if (matches.stream().noneMatch(ft -> isAllowedType(ft.mimeType(), allowedTypes))) {
      throw new FileUploadTypeException(
          String.format(
              "File \"%s\": detected file type \"%s\" is not an allowed upload type.",
              fileName, matches.get(0).mimeType()));
    }

    Optional<FileType> declaredMatch =
        matches.stream().filter(ft -> ft.mimeType().equals(declaredNormalized)).findFirst();
    if (declaredMatch.isEmpty()) {
      throw new FileUploadTypeException(
          String.format(
              "File \"%s\": declared content type \"%s\" does not match detected type \"%s\".",
              fileName, declaredContentType, matches.get(0).mimeType()));
    }

    String extension = extensionOf(fileName);
    if (extension != null && !declaredMatch.get().extensions().contains(extension)) {
      throw new FileUploadTypeException(
          String.format(
              "File \"%s\": filename extension \"%s\" does not match detected type \"%s\".",
              fileName, extension, declaredMatch.get().mimeType()));
    }
  }

  private static String extensionOf(String fileName) {
    if (fileName == null) {
      return null;
    }
    int dot = fileName.lastIndexOf('.');
    if (dot < 0) {
      return null;
    }
    return fileName.substring(dot).toLowerCase(Locale.ROOT);
  }

  static ImmutableList<FileType> detectTypes(ByteString headerBytes) {
    if (headerBytes == null) {
      return ImmutableList.of();
    }
    boolean isWebp = isRiffWebp(headerBytes);
    return FILE_TYPES.stream()
        .filter(ft -> ft.matches(headerBytes))
        // The WEBP entry's magic is just "RIFF", a generic container header.
        // Only accept the match when bytes 8-11 also spell "WEBP".
        .filter(ft -> !"image/webp".equals(ft.mimeType()) || isWebp)
        .collect(ImmutableList.toImmutableList());
  }

  private static boolean isRiffWebp(ByteString bytes) {
    return bytes.length() >= 12
        && bytes.apply(8) == 0x57
        && bytes.apply(9) == 0x45
        && bytes.apply(10) == 0x42
        && bytes.apply(11) == 0x50;
  }

  /** Parses a comma-separated list of MIME types, wildcards, and extensions. */
  public static ImmutableList<String> parseSpecifiers(String specifiers) {
    return Splitter.on(',')
        .trimResults()
        .omitEmptyStrings()
        .splitToStream(specifiers)
        .map(spec -> spec.toLowerCase(Locale.ROOT))
        .flatMap(spec -> normalizeSpecifier(spec).stream())
        .collect(ImmutableList.toImmutableList());
  }

  private static Optional<String> normalizeSpecifier(String normalized) {
    if (normalized.endsWith("/*")) {
      return Optional.of(normalized);
    }
    if (normalized.startsWith(".")) {
      return fromExtension(normalized).map(FileType::mimeType);
    }
    return Optional.of(normalized);
  }

  private static boolean isAllowedType(String normalizedMime, ImmutableList<String> allowedTypes) {
    if (normalizedMime == null || normalizedMime.isEmpty()) {
      return false;
    }
    return allowedTypes.stream()
        .anyMatch(
            allowed ->
                allowed.endsWith("/*")
                    ? normalizedMime.startsWith(allowed.substring(0, allowed.length() - 1))
                    : normalizedMime.equals(allowed));
  }

  static Optional<FileType> fromExtension(String extension) {
    if (extension == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(FILE_TYPE_BY_EXTENSION.get(extension.toLowerCase(Locale.ROOT)));
  }

  /** Represents a file type with one or more magic byte signatures. */
  record FileType(
      ImmutableList<String> extensions,
      String mimeType,
      ImmutableList<ByteString> magicSignatures) {

    static FileType of(ImmutableList<String> extensions, String mimeType, byte[][] signatures) {
      ImmutableList<ByteString> sigs =
          Arrays.stream(signatures)
              .map(sig -> ByteString.fromArray(Arrays.copyOf(sig, sig.length)))
              .collect(ImmutableList.toImmutableList());
      return new FileType(extensions, mimeType, sigs);
    }

    boolean matches(ByteString data) {
      return magicSignatures.stream().anyMatch(sig -> data.startsWith(sig, 0));
    }
  }
}
