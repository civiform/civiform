package views.admin;

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
 */
@Builder
public record ScriptElementSettings(
    String src, String type, boolean async, boolean defer, String integrity, String id) {

  /** Compact constructor with validation. */
  public ScriptElementSettings {
    if (src == null || src.isBlank()) {
      throw new IllegalArgumentException("'src' must not be null or empty");
    }

    if (async && defer) {
      throw new IllegalArgumentException("'async' and 'defer' should not both be true");
    }

    if (type == null || type.isBlank()) {
      type = "module";
    }
  }
}
