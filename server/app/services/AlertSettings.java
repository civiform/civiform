package services;

import java.util.Optional;

/**
 * Contains settings
 *
 * @param show Determines if the alert be displayed or not
 * @param title Alert title, if any
 * @param text Alert text
 * @param alertType {@link AlertType}
 */
public record AlertSettings(
    Boolean show, Optional<String> title, String text, AlertType alertType) {
  public static AlertSettings empty() {
    return new AlertSettings(false, Optional.empty(), "", AlertType.NONE);
  }
}
