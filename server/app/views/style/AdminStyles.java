package views.style;

/** Styles for admin pages. */
public final class AdminStyles {

  public static final String LANGUAGE_LINK_SELECTED =
      StyleUtils.joinStyles(
          ReferenceClasses.ADMIN_LANGUAGE_LINK, "m-2", "border-blue-400", "border-b-2");

  public static final String LANGUAGE_LINK_NOT_SELECTED =
      StyleUtils.joinStyles(ReferenceClasses.ADMIN_LANGUAGE_LINK, "m-2");

  public static final String ADMIN_NAV_BAR_LOGO =
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
      StyleUtils.joinStyles("bg-gradient-to-r", "from-gray-100", "via-white", "to-gray-100");

  public static final String NAV_STYLES =
      StyleUtils.joinStyles(
          "shadow",
          "z-10",
          "bg-white",
          "text-gray-700",
          "h-12",
          "fixed",
          "top-0",
          "w-screen",
          "px-4",
          "py-3");

  public static final String MOVE_BLOCK_BUTTON =
      StyleUtils.joinStyles(
          "bg-transparent",
          "p-0",
          "w-6",
          "text-center",
          "text-gray-500",
          StyleUtils.hover("bg-gray-200", "text-gray-900"));

  public static final String BODY =
      StyleUtils.joinStyles(BODY_GRADIENT_STYLE, "box-border", "flex", "min-h-screen");

  public static final String MAIN_CENTERED =
      StyleUtils.joinStyles("px-2", "max-w-screen-2xl", "mx-auto");

  public static final String MAIN_FULL = StyleUtils.joinStyles("flex", "flex-row");

  public static final String MAIN =
      StyleUtils.joinStyles(
          "bg-white", "border", "border-gray-200", "mt-12", "shadow-lg", "w-screen");
}
