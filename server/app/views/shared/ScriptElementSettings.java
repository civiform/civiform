package views.shared;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Set;
import lombok.Builder;

/**
 * Settings for an HTML5 <script> element.
 *
 * @param src The URL of an external script file (src attribute)
 * @param type The MIME type or module type (type attribute, e.g., "text/javascript", "module")
 * @param async Whether the script should execute asynchronously (async attribute)
 * @param defer Whether the script should execute after document parsing (defer attribute)
 * @param integrity Subresource integrity hash for verifying fetched resources
 * @param id The id attribute for the script element
 * @param crossOrigin Cross-Origin Resource Sharing settings
 */
@Builder
public record ScriptElementSettings(
    String src,
    String type,
    boolean async,
    boolean defer,
    String integrity,
    String id,
    String crossOrigin) {

  private static final Set<String> VALID_CROSS_ORIGIN_VALUES =
      Set.of("anonymous", "use-credentials");

  /** Compact constructor with validation. */
  public ScriptElementSettings {
    if (isBlank(src)) {
      throw new IllegalArgumentException("'src' must not be null or empty");
    }

    if (async && defer) {
      throw new IllegalArgumentException("'async' and 'defer' should not both be true");
    }

    if (isBlank(type)) {
      type = "module";
    }

    if (!isBlank(crossOrigin) && !VALID_CROSS_ORIGIN_VALUES.contains(crossOrigin)) {
      throw new IllegalArgumentException(
          "'crossOrigin' must be one of %s".formatted(VALID_CROSS_ORIGIN_VALUES));
    }

    // Both modules and text/javascript default to anonymous CORS.
    // Modules always use CORS; text/javascript needs it explicit for cross-origin fetches.
    if (isBlank(crossOrigin)) {
      crossOrigin = "anonymous";
    }
  }
}
