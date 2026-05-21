package parsers;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allowlist extensions for {@link FileTypeValidation}. Each constant defines a supported extension
 * or a wildcard such as {@code image/*}) and the MIME type or wildcard used when checking uploads.
 */
@Getter
public enum FileTypeSpecifier {
  IMAGE_WILDCARD("image/*", "image/*"),

  PNG(".png", "image/png"),
  JPG(".jpg", "image/jpeg"),
  JPEG(".jpeg", "image/jpeg"),
  GIF(".gif", "image/gif"),
  PDF(".pdf", "application/pdf"),
  BMP(".bmp", "image/bmp"),
  WEBP(".webp", "image/webp"),
  TIFF(".tiff", "image/tiff");

  private static final Logger logger = LoggerFactory.getLogger(FileTypeSpecifier.class);

  static final ImmutableMap<String, String> MIME_BY_EXTENSION_MAP = buildMimeByExtension();

  private final String extension;
  private final String mimeType;

  FileTypeSpecifier(String extension, String mimeType) {
    String extensionValue = checkNotNull(extension, "extension");
    String mimeTypeValue = checkNotNull(mimeType, "mimeType");
    if (extensionValue.isEmpty() || mimeTypeValue.isEmpty()) {
      throw new IllegalArgumentException("extension and mimeType must be non-empty");
    }
    this.extension = extensionValue;
    this.mimeType = mimeTypeValue;
  }

  private static ImmutableMap<String, String> buildMimeByExtension() {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    for (FileTypeSpecifier specifier : values()) {
      if (specifier.getExtension().startsWith(".")) {
        builder.put(specifier.getExtension(), specifier.getMimeType());
      }
    }
    return builder.build();
  }

  /**
   * Parses a comma-separated allowlist. Extension and wildcard tokens match one constant; a MIME
   * token matches every constant with that MIME (e.g. {@code image/jpeg} yields both {@link #JPG}
   * and {@link #JPEG}).
   */
  public static ImmutableList<FileTypeSpecifier> parseCommaSeparated(String specifiers) {
    ImmutableList.Builder<FileTypeSpecifier> builder = ImmutableList.builder();
    for (String allowlistPart :
        Splitter.on(',').trimResults().omitEmptyStrings().split(specifiers)) {
      String normalizedAllowlistPart = allowlistPart.toLowerCase(Locale.ROOT);
      FileTypeSpecifier extensionMatch = null;
      for (FileTypeSpecifier specifier : values()) {
        if (specifier.getExtension().equals(normalizedAllowlistPart)) {
          extensionMatch = specifier;
          break;
        }
      }
      if (extensionMatch != null) {
        builder.add(extensionMatch);
        continue;
      }
      ImmutableList.Builder<FileTypeSpecifier> mimeMatches = ImmutableList.builder();
      for (FileTypeSpecifier specifier : values()) {
        if (specifier.getMimeType().equals(normalizedAllowlistPart)) {
          mimeMatches.add(specifier);
        }
      }
      ImmutableList<FileTypeSpecifier> mimeList = mimeMatches.build();
      if (mimeList.isEmpty()) {
        logger.error("Unknown file type specifier: {}", allowlistPart);
        continue;
      }
      builder.addAll(mimeList);
    }
    return builder.build();
  }

  /** MIME or wildcard string used by {@link FileTypeValidation} allowlist checks. */
  String normalizedAllowEntry() {
    return getMimeType();
  }
}
