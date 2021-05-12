package views.style;

public final class ApplicantStyles {
  public static final String BODY_BG_COLOR = "bg-beige";

  public static final String BODY =
      StyleUtils.joinStyles(BODY_BG_COLOR, Styles.H_FULL, Styles.W_FULL);

  public static final String LOGO_STYLE =
      StyleUtils.joinStyles(Styles.TEXT_2XL, Styles.TEXT_RED_400);
}
