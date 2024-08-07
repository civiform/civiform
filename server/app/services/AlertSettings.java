package services;

import com.google.common.collect.ImmutableList;
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
    Boolean show,
    Optional<String> title,
    String text,
    AlertType alertType,
    ImmutableList<String> additionalText) {
  public static AlertSettings empty() {
    return new AlertSettings(false, Optional.empty(), "", AlertType.NONE);
  }

  public AlertSettings(Boolean show, Optional<String> title, String text, AlertType alertType) {
    this(show, title, text, alertType, ImmutableList.of());
  }
}
