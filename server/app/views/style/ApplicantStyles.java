package views.style;

/** Styles for applicant pages. */
public final class ApplicantStyles {
  public static final String BODY_BG_COLOR = BaseStyles.BG_CIVIFORM_WHITE;
  public static final String BODY =
      StyleUtils.joinStyles(BODY_BG_COLOR, "h-full", "w-full", "flex", "flex-col");

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

  public static final String PROGRAM_TITLE =
      StyleUtils.joinStyles(BaseStyles.TEXT_CIVIFORM_BLUE, "text-lg", "font-bold");
}
