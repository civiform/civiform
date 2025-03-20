package services;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import views.components.TextFormatter;

/**
 * Contains settings
 *
 * @param show Determines if the alert be displayed or not
 * @param title Alert title, if any
 * @param text Alert text
 * @param unescapedDescription true to use an unescaped description (th:utext). false otherwise.
 * @param alertType {@link AlertType}
 */
public record AlertSettings(
    Boolean show,
    Optional<String> title,
    String text,
    Boolean unescapedDescription,
    AlertType alertType,
    ImmutableList<String> additionalText,
    Boolean isSlim) {

  public static AlertSettings empty() {
    return AlertSettings.builder().build();
  }

  public static AlertSettingsBuilder builder() {
    return new AlertSettingsBuilder();
  }

  /** Sanitized HTML for the alert text that processes Markdown. */
  public String getFormattedAlertText(String text) {
    return TextFormatter.formatTextToSanitizedHTML(
        text, /* preserveEmptyLines= */ false, /* addRequiredIndicator= */ false);
  }

  public static final class AlertSettingsBuilder {
    private Boolean show = false;
    private Optional<String> title = Optional.empty();
    private String text = "";
    private Boolean unescapedDescription = true;
    private AlertType alertType = AlertType.NONE;
    private ImmutableList<String> additionalText = ImmutableList.of();
    private Boolean isSlim = false;

    public AlertSettingsBuilder show(Boolean show) {
      this.show = show;
      return this;
    }

    public AlertSettingsBuilder title(Optional<String> title) {
      this.title = title;
      return this;
    }

    public AlertSettingsBuilder text(String text) {
      this.text = text;
      return this;
    }

    public AlertSettingsBuilder unescapedDescription(Boolean unescapedDescription) {
      this.unescapedDescription = unescapedDescription;
      return this;
    }

    public AlertSettingsBuilder alertType(AlertType alertType) {
      this.alertType = alertType;
      return this;
    }

    public AlertSettingsBuilder additionalText(ImmutableList<String> additionalText) {
      this.additionalText = additionalText;
      return this;
    }

    public AlertSettingsBuilder isSlim(Boolean isSlim) {
      this.isSlim = isSlim;
      return this;
    }

    public AlertSettings build() {
      return new AlertSettings(
          show, title, text, unescapedDescription, alertType, additionalText, isSlim);
    }
  }
}
