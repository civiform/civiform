package views.admin.questions;

import com.google.common.collect.ImmutableList;

/**
 * YES_NO question settings. {@code showLabel} is true when rendering the default option set for a
 * question with no saved options.
 */
public record YesNoConfig(boolean showLabel, ImmutableList<YesNoOptionRow> options) {

  /**
   * A single YES_NO option row: hidden form-binding inputs plus a display checkbox. Required
   * options ("yes"/"no") render checked and disabled, with an extra hidden input so the value still
   * posts.
   */
  public record YesNoOptionRow(
      String optionId,
      String adminName,
      String optionText,
      boolean required,
      boolean checked,
      String ariaLabel) {}
}
