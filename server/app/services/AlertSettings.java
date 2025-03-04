package services;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import play.i18n.Messages;
import views.components.TextFormatter;

/**
 * Contains settings
 *
 * @param show Determines whether the alert is displayed
 * @param title Title, if any
 * @param text Description text
 * @param unescapedDescription true to use an unescaped description (th:utext). false otherwise.
 * @param alertType {@link AlertType}
 * @param additionalText Additional text to be displayed as a list
 * @param customText Customized text added by the admin, if any
 * @param ariaLabel Optional help text for screen readers
 * @param isSlim Determines whether the alert should have slim layout
 */
public record AlertSettings(
    Boolean show,
    Optional<String> title,
    String text,
    Boolean unescapedDescription,
    AlertType alertType,
    ImmutableList<String> additionalText,
    Optional<String> customText,
    Optional<String> ariaLabel,
    Boolean isSlim) {

  public static AlertSettings empty() {
    return new AlertSettings(false, Optional.empty(), "", AlertType.NONE);
  }

  public AlertSettings(Boolean show, Optional<String> title, String text, AlertType alertType) {
    this(show, title, text, alertType, ImmutableList.of(), /* isSlim= */ false);
  }

  public AlertSettings(
      Boolean show,
      Optional<String> title,
      String text,
      AlertType alertType,
      ImmutableList<String> additionalText,
      Boolean isSlim) {
    this(
        show,
        title,
        text,
        /* unescapedDescription= */ true,
        alertType,
        additionalText,
        /* customText= */ Optional.empty(),
        /* ariaLabel= */ Optional.empty(),
        isSlim);
  }

  public AlertSettings(
      Boolean show,
      Optional<String> title,
      String text,
      AlertType alertType,
      ImmutableList<String> additionalText,
      Optional<String> customText,
      Optional<String> ariaLabel,
      Boolean isSlim) {
    this(
        show,
        title,
        text,
        /* unescapedDescription= */ true,
        alertType,
        additionalText,
        customText,
        ariaLabel,
        isSlim);
  }

  /** Sanitized HTML for the alert text that processes Markdown. */
  public String getFormattedAlertText(String text) {
    return TextFormatter.formatTextToSanitizedHTML(
        text, /* preserveEmptyLines= */ false, /* addRequiredIndicator= */ false);
  }

  public static String getTitleAriaLabel(Messages messages, AlertType alertType, String titleText) {
    switch (alertType) {
      case SUCCESS:
        return messages.at(MessageKey.HEADING_SUCCESS_ARIA_LABEL_PREFIX.getKeyName(), titleText);
      case INFO:
        return messages.at(
            MessageKey.HEADING_INFORMATION_ARIA_LABEL_PREFIX.getKeyName(), titleText);
      default:
        return titleText;
    }
  }
}
