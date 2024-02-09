package views.style;

/** Styles for applicant pages. */
public final class ApplicantStyles {
  public static final String BODY_BG_COLOR = BaseStyles.BG_CIVIFORM_WHITE;
  public static final String BODY =
      StyleUtils.joinStyles(BODY_BG_COLOR, "h-full", "w-full", "flex", "flex-col");

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
      StyleUtils.joinStyles("bg-blue-900", "text-white", "text-center", "w-full");

  public static final String CIVIFORM_LOGO =
      StyleUtils.joinStyles(
          "text-3xl",
          "opacity-75",
          "flex",
          "flex-wrap",
          "content-center",
          StyleUtils.hover("opacity-100"));
  public static final String LINK =
      StyleUtils.joinStyles(
          "text-sm",
          "text-blue-900",
          "font-bold",
          "opacity-75",
          "underline",
          StyleUtils.hover("opacity-100"));

  public static final String PROGRAM_APPLICATION_TITLE =
      StyleUtils.joinStyles("text-3xl", "text-black", "font-bold", "mt-8", "mb-4");
  public static final String PROGRAM_TITLE =
      StyleUtils.joinStyles(BaseStyles.TEXT_CIVIFORM_BLUE, "text-lg", "font-bold");

  public static final String PROGRAM_CARDS_SUBTITLE =
      StyleUtils.joinStyles("my-4", "text-lg", "px-4");
  public static final String PROGRAM_CARDS_CONTAINER_BASE =
      StyleUtils.joinStyles(
          "grid",
          "grid-cols-1",
          "gap-4",
          //"justify-between",
          "items-start",
          StyleUtils.responsiveSmall("grid-cols-1"),
          StyleUtils.responsiveLarge("gap-8"));
  public static final String PROGRAM_CARD =
      StyleUtils.joinStyles(
          // This width is closely tied to the grid layout in ProgramIndexView.java. Increasing the
          // card size may cause them to overlap on smaller screen sizes.
          "w-72",
          "bg-white",
          "rounded-xl",
          "shadow-md",
          "border",
          "border-gray-200",
          "flex",
          "flex-col");

  public static final String PROGRAM_INFORMATION_BOX =
      StyleUtils.joinStyles(
          "border",
          "border-gray-200",
          "rounded-2xl",
          "shadow-md",
          "bg-white",
          "p-4",
          StyleUtils.responsiveSmall("p-6"),
          "my-6");

  public static final String QUESTION_TEXT =
      StyleUtils.joinStyles("text-black", "text-xl", "font-bold", "mb-2");
  public static final String QUESTION_HELP_TEXT = StyleUtils.joinStyles("text-black", "text-xl");

  public static final String APPLICATION_NAV_BAR =
      StyleUtils.joinStyles(
          "gap-4",
          "flex",
          "flex-col",
          StyleUtils.responsiveMedium("flex-row"),
          "justify-end",
          "flex-wrap");
}
