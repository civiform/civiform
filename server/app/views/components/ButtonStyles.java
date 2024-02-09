package views.components;

import views.style.ApplicantStyles;
import views.style.BaseStyles;
import views.style.StyleUtils;

/**
 * A collection of styles for buttons throughout CiviForm. Importantly, the public style Strings in
 * this file should be **generalizable**, meaning they are not designed for a particuar use case.
 * Styles should not be called i.e. BUTTON_SUBMIT_APPLICATION for example. If a specific change is
 * required for a use case, use a general style and add more classes in the client code.
 */
public final class ButtonStyles {
  /**
   * Base styles for buttons in CiviForm. This is missing a specified text size, so that should be
   * added by other button style constants that use this as a base.
   */
  private static final String BUTTON_BASE =
      StyleUtils.joinStyles("block", "py-2", "text-center", "rounded-full", "border");

  /** Base styles for semibold buttons with a solid blue background. */
  private static final String BUTTON_BASE_SOLID_BLUE_SEMIBOLD =
      StyleUtils.joinStyles(
          BUTTON_BASE,
          BaseStyles.BG_CIVIFORM_BLUE,
          "border-transparent",
          "text-white",
          "rounded-full",
          "font-semibold",
          "px-8",
          StyleUtils.hover("bg-blue-700"),
          StyleUtils.disabled("bg-gray-200", "text-gray-400"));

  private static final String BUTTON_BASE_OUTLINE_SEMIBOLD =
      StyleUtils.joinStyles(
          "font-semibold",
          "px-8",
          BUTTON_BASE,
          "bg-transparent",
          BaseStyles.TEXT_CIVIFORM_BLUE,
          BaseStyles.BORDER_CIVIFORM_BLUE,
          StyleUtils.hover("bg-blue-100"));

  // ---------------- CLIENT-FACING STYLES BELOW ----------------

  public static final String SOLID_WHITE =
      StyleUtils.joinStyles(
          BUTTON_BASE_OUTLINE_SEMIBOLD, BaseStyles.BG_CIVIFORM_WHITE, "text-blue-900");

  public static final String SOLID_BLUE =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_BLUE_SEMIBOLD, "text-base");

  public static final String SOLID_BLUE_WITH_ICON =
      StyleUtils.joinStyles(
          // Buttons with icons should not have extra padding
          StyleUtils.removeStyles(BUTTON_BASE_SOLID_BLUE_SEMIBOLD, "px-8"),
          "text-base",
          "space-x-2");

  public static final String SOLID_BLUE_TEXT_SM =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_BLUE_SEMIBOLD, "text-sm");

  public static final String SOLID_BLUE_TEXT_XL =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_BLUE_SEMIBOLD, "text-xl");

  public static final String OUTLINED_TRANSPARENT =
      StyleUtils.joinStyles(BUTTON_BASE_OUTLINE_SEMIBOLD, "text-base");

  public static final String OUTLINED_WHITE_WITH_ICON =
      StyleUtils.joinStyles(
          // Buttons with icons should not have extra padding
          StyleUtils.removeStyles(BUTTON_BASE_OUTLINE_SEMIBOLD, "px-8", "bg-transparent"),
          "flex",
          "items-center",
          "font-medium",
          "space-x-2",
          "bg-white",
          StyleUtils.hover("bg-gray-200"));

  public static final String CLEAR_WITH_ICON =
      StyleUtils.joinStyles(
          "flex",
          "items-center",
          "font-medium",
          "space-x-2",
          "border-none",
          "rounded-full",
          "bg-transparent",
          BaseStyles.TEXT_CIVIFORM_BLUE,
          StyleUtils.hover("bg-gray-200"));

  // Just like CLEAR_WITH_ICON, but in dropdowns we want to remove the rounded corners.
  public static final String CLEAR_WITH_ICON_FOR_DROPDOWN =
      StyleUtils.removeStyles(CLEAR_WITH_ICON, "rounded-full");

  // Use the base link styles and override default background, border, and hover button styles
  public static final String LINK_STYLE =
      StyleUtils.joinStyles(
          StyleUtils.removeStyles(ApplicantStyles.LINK, "text-sm"),
          "bg-transparent",
          "border-none",
          "hover:bg-opacity-0");
}
