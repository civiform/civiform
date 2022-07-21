package views.style;

/** Styles for admin pages. */
public class AdminStyles {

  public static final String LANGUAGE_LINK_SELECTED =
      StyleUtils.joinStyles(
          ReferenceClasses.ADMIN_LANGUAGE_LINK,
          "m-2",
          "border-blue-400",
          "border-b-2");

  public static final String LANGUAGE_LINK_NOT_SELECTED =
      StyleUtils.joinStyles(ReferenceClasses.ADMIN_LANGUAGE_LINK, "m-2");

  public static final String ADMIN_NAV_BAR =
      StyleUtils.joinStyles(
          "absolute",
          "bg-contain",
          "bg-gray-700",
          "h-7",
          "left-5",
          "m-1",
          "opacity-75",
          "text-center",
          "text-lg",
          "text-white",
          "top-2",
          "w-7",
          "rounded");

  public static final String BODY_GRADIENT_STYLE =
      StyleUtils.joinStyles(
          "bg-gradient-to-r", "from-gray-100", "via-white", "to-gray-100");

  public static final String NAV_BACKGROUND_COLOR = "bg-white";
  public static final String NAV_TEXT_COLOR = "text-gray-700";
  public static final String NAV_HEIGHT = "h-12";
  public static final String NAV_FIXED =
      StyleUtils.joinStyles("fixed", "top-0", "w-screen");
  public static final String NAV_PADDING = StyleUtils.joinStyles("px-4", "py-3");
  public static final String NAV_STYLES =
      StyleUtils.joinStyles(
          "shadow", NAV_BACKGROUND_COLOR, NAV_TEXT_COLOR, NAV_HEIGHT, NAV_FIXED, NAV_PADDING);

  /** Invisible buttons covering an area that are used for form submit. */
  public static final String CLICK_TARGET_BUTTON =
      StyleUtils.joinStyles(
          "absolute",
          "h-full",
          "left-0",
          "opacity-0",
          "top-0",
          "w-full");

  public static final String MOVE_BLOCK_BUTTON =
      StyleUtils.joinStyles(
          "bg-transparent",
          "p-0",
          "w-6",
          "text-center",
          "text-gray-500",
          StyleUtils.hover("bg-gray-200", "text-gray-900"));

  public static final String BODY =
      StyleUtils.joinStyles(
          BODY_GRADIENT_STYLE,
          "box-border",
          "h-screen",
          "w-screen",
          "overflow-hidden",
          "flex");

  public static final String MAIN_CENTERED =
      StyleUtils.joinStyles("px-2", "max-w-screen-2xl", "mx-auto");

  public static final String MAIN_FULL = StyleUtils.joinStyles("flex", "flex-row");

  public static final String MAIN =
      StyleUtils.joinStyles(
          "bg-white",
          "border",
          "border-gray-200",
          "mt-12",
          "overflow-y-auto",
          "shadow-lg",
          "w-screen");

  public static final String BUTTON_QUESTION_PREDICATE =
      StyleUtils.joinStyles(
          "w-full",
          "px-4",
          "py-2",
          "border",
          "border-gray-200",
          "text-black",
          "text-left",
          "font-normal",
          "bg-white",
          StyleUtils.hover("text-gray-800", "bg-gray-100"));

  private static final String BASE_BUTTON_STYLES =
      StyleUtils.joinStyles("flex", "items-center", "font-medium");

  public static final String PRIMARY_BUTTON_STYLES =
      StyleUtils.joinStyles(
          BASE_BUTTON_STYLES,
          "rounded-full",
          "space-x-2",
          BaseStyles.BG_SEATTLE_BLUE,
          "text-white");

  public static final String SECONDARY_BUTTON_STYLES =
      StyleUtils.joinStyles(
          BASE_BUTTON_STYLES,
          "rounded-full",
          "space-x-2",
          "border",
          BaseStyles.BORDER_SEATTLE_BLUE,
          "bg-white",
          BaseStyles.TEXT_SEATTLE_BLUE,
          StyleUtils.hover("bg-gray-200"));

  public static final String TERTIARY_BUTTON_STYLES =
      StyleUtils.joinStyles(
          BASE_BUTTON_STYLES,
          "space-x-2",
          "border-none",
          "rounded",
          "bg-transparent",
          BaseStyles.TEXT_SEATTLE_BLUE,
          StyleUtils.hover("bg-gray-200"));

  public static final String DROPDOWN_BUTTON_STYLES =
      StyleUtils.joinStyles(
          BASE_BUTTON_STYLES,
          "space-x-4",
          "border-none",
          "rounded",
          "bg-transparent",
          BaseStyles.TEXT_SEATTLE_BLUE,
          StyleUtils.hover("bg-gray-200"));
}
