package views.components;

import views.style.BaseStyles;
import views.style.StyleUtils;

/** A collection of styles for buttons throughout CiviForm. */
public final class ButtonStyles {
  /**
   * Base styles for buttons in the applicant UI. This is missing a specified text size, so that
   * should be added by other button style constants that use this as a base.
   */
  private static final String BUTTON_BASE =
      StyleUtils.joinStyles(
          "block", "py-2", "text-center", "rounded-full", "border", "border-transparent");

  /** Base styles for buttons with a solid background color. */
  private static final String BUTTON_BASE_SOLID =
      StyleUtils.joinStyles(
          BUTTON_BASE,
          BaseStyles.BG_SEATTLE_BLUE,
          "text-white",
          "rounded-full",
          StyleUtils.hover("bg-blue-700"),
          StyleUtils.disabled("bg-gray-200", "text-gray-400"));

  /** Base styles for semibold buttons with a solid background. */
  private static final String BUTTON_BASE_SOLID_SEMIBOLD =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID, "font-semibold", "px-8");

  /** Base styles for buttons with a transparent background and an outline. */
  private static final String BUTTON_BASE_OUTLINE =
      StyleUtils.joinStyles(
          // Remove "border-transparent" so it doesn't conflict with "border-seattle-blue".
          StyleUtils.removeStyles(BUTTON_BASE, "border-transparent"),
          "bg-transparent",
          BaseStyles.TEXT_SEATTLE_BLUE,
          BaseStyles.BORDER_SEATTLE_BLUE,
          StyleUtils.hover("bg-blue-100"));

  private static final String BUTTON_BASE_OUTLINE_SEMIBOLD =
      StyleUtils.joinStyles(BUTTON_BASE_OUTLINE, "font-semibold", "px-8");

  // ---------------- CLIENT-FACING STYLES BELOW ----------------

  public static final String SOLID_WHITE =
      StyleUtils.joinStyles(
          BUTTON_BASE_OUTLINE_SEMIBOLD, BaseStyles.BG_CIVIFORM_WHITE, "text-blue-900");

  public static final String SOLID_BLUE =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_SEMIBOLD, "text-base");

  public static final String SOLID_BLUE_TEXT_SM =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_SEMIBOLD, "text-sm");

  public static final String SOLID_BLUE_TEXT_XL =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_SEMIBOLD, "text-xl");

  public static final String OUTLINED_TRANSPARENT =
      StyleUtils.joinStyles(BUTTON_BASE_OUTLINE_SEMIBOLD, "text-base");
}
