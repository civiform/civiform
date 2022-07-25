package views.style;

/** Styles for applicant pages. */
public final class ApplicantStyles {
  public static final String BODY_BG_COLOR = BaseStyles.BG_CIVIFORM_WHITE;
  public static final String BODY =
      StyleUtils.joinStyles(BODY_BG_COLOR, "h-full", "w-full");

  public static final String MAIN_APPLICANT_INFO =
      StyleUtils.joinStyles("w-5/6", "max-w-screen-sm", "mx-auto", "my-8");
  public static final String MAIN_PROGRAM_APPLICATION =
      StyleUtils.joinStyles(
          "w-5/6",
          StyleUtils.responsiveSmall("w-2/3"),
          "mx-auto",
          "my-8",
          StyleUtils.responsiveSmall("my-12"));

  public static final String PROGRAM_INDEX_TOP_CONTENT =
      StyleUtils.joinStyles(
          BaseStyles.BG_SEATTLE_BLUE, "text-white", "text-center", "w-full");

  public static final String CIVIFORM_LOGO =
      StyleUtils.joinStyles(
          "text-2xl", "opacity-75", StyleUtils.hover("opacity-100"));
  public static final String LINK_LOGOUT =
      StyleUtils.joinStyles(
          "text-base",
          "font-bold",
          "opacity-75",
          StyleUtils.hover("opacity-100"));

  public static final String H1_PROGRAM_APPLICATION =
      StyleUtils.joinStyles(
          "text-3xl", "text-black", "font-bold", "mt-8", "mb-4");
  public static final String H2_PROGRAM_TITLE =
      StyleUtils.joinStyles(BaseStyles.TEXT_SEATTLE_BLUE, "text-lg", "font-bold");

  public static final String PROGRAM_CARDS_SUBTITLE =
      StyleUtils.joinStyles("my-4", "text-lg", "px-4");
  public static final String PROGRAM_CARDS_CONTAINER_BASE =
      StyleUtils.joinStyles(
          "grid",
          "grid-cols-1",
          "gap-4",
          "place-items-center",
          StyleUtils.responsiveSmall("grid-cols-1"),
          StyleUtils.responsiveLarge("gap-8"));
  public static final String PROGRAM_CARD =
      StyleUtils.joinStyles(
          "w-full",
          StyleUtils.responsiveSmall("w-72"),
          "h-72",
          "bg-white",
          "rounded-xl",
          "shadow-md",
          "border",
          "border-gray-200",
          "flex",
          "flex-col",
          "gap-4");

  public static final String QUESTION_TEXT =
      StyleUtils.joinStyles("text-black", "text-xl", "font-bold", "mb-2");
  public static final String QUESTION_HELP_TEXT =
      StyleUtils.joinStyles("text-black", "text-xl");

  public static final String APPLICATION_NAV_BAR =
      StyleUtils.joinStyles(
          "gap-4", "flex", "flex-col", StyleUtils.responsiveMedium("flex-row"));

  /**
   * Base styles for buttons in the applicant UI. This is missing a specified text size, so that
   * should be added by other button style constants that use this as a base.
   */
  private static final String BUTTON_BASE =
      StyleUtils.joinStyles(
          "block",
          "py-2",
          "text-center",
          "rounded-full",
          "border",
          "border-transparent");

  /** Base styles for buttons with a solid background color. */
  private static final String BUTTON_BASE_SOLID =
      StyleUtils.joinStyles(
          BUTTON_BASE,
          BaseStyles.BG_SEATTLE_BLUE,
          "text-white",
          "rounded-full",
          StyleUtils.hover("bg-blue-700"),
          StyleUtils.disabled("bg-gray-200", "text-gray-400"));

  /** Base styles for semibold, upper case buttons with a solid background. */
  private static final String BUTTON_BASE_SOLID_UPPERCASE =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID, "uppercase", "font-semibold", "px-8");

  /** Base styles for buttons with a transparent background and an outline. */
  private static final String BUTTON_BASE_OUTLINE =
      StyleUtils.joinStyles(
          BUTTON_BASE,
          "bg-transparent",
          BaseStyles.TEXT_SEATTLE_BLUE,
          "border",
          BaseStyles.BORDER_SEATTLE_BLUE,
          StyleUtils.hover("bg-blue-100"));

  private static final String BUTTON_BASE_OUTLINE_UPPERCASE =
      StyleUtils.joinStyles(
          BUTTON_BASE_OUTLINE, "uppercase", "font-semibold", "px-8");

  public static final String BUTTON_SELECT_LANGUAGE =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_UPPERCASE, "text-base", "mx-auto");
  public static final String BUTTON_PROGRAM_APPLY =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_UPPERCASE, "text-sm", "mx-auto");
  public static final String BUTTON_BLOCK_NEXT =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_UPPERCASE, "text-base");
  public static final String BUTTON_BLOCK_PREVIOUS =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_UPPERCASE, "text-base");
  public static final String BUTTON_REVIEW =
      StyleUtils.joinStyles(BUTTON_BASE_OUTLINE_UPPERCASE, "text-base");
  public static final String BUTTON_SUBMIT_APPLICATION =
      StyleUtils.joinStyles(
          BUTTON_BASE_SOLID_UPPERCASE, "text-base", "mx-auto", "bg-green-700");
  public static final String BUTTON_ENUMERATOR_ADD_ENTITY =
      StyleUtils.joinStyles(
          BUTTON_BASE_SOLID, "text-base", "normal-case", "font-normal", "px-4");
  public static final String BUTTON_ENUMERATOR_REMOVE_ENTITY =
      StyleUtils.joinStyles(
          BUTTON_BASE_OUTLINE,
          "text-base",
          "normal-case",
          "font-normal",
          "justify-self-end",
          "self-center");
  public static final String BUTTON_CREATE_ACCOUNT =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_UPPERCASE, "text-base");
  public static final String BUTTON_NOT_RIGHT_NOW =
      StyleUtils.joinStyles(BUTTON_BASE_OUTLINE_UPPERCASE, "text-base");
}
