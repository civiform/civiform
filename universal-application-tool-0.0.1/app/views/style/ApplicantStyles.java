package views.style;

public final class ApplicantStyles {
  public static final String BODY_BG_COLOR = BaseStyles.BG_CIVIFORM_WHITE;
  public static final String BODY =
      StyleUtils.joinStyles(BODY_BG_COLOR, Styles.H_FULL, Styles.W_FULL);

  public static final String MAIN = StyleUtils.joinStyles();
  public static final String MAIN_PROGRAM_APPLICATION =
      StyleUtils.joinStyles(Styles.W_2_3, Styles.M_AUTO);

  public static final String LOGO_STYLE = StyleUtils.joinStyles(Styles.TEXT_2XL);

  public static final String QUESTION_TEXT =
      StyleUtils.joinStyles(Styles.TEXT_BLACK, Styles.TEXT_XL, Styles.FONT_BOLD, Styles.MB_2);
  public static final String QUESTION_HELP_TEXT =
      StyleUtils.joinStyles(Styles.TEXT_BLACK, Styles.TEXT_XL);

  public static final String H1_PROGRAM_APPLICATION =
      StyleUtils.joinStyles(
          Styles.TEXT_3XL, Styles.TEXT_BLACK, Styles.FONT_BOLD, Styles.MT_8, Styles.MB_4);
  public static final String BLOCK_HEADING = H1_PROGRAM_APPLICATION;

  public static final String BUTTON_BLOCK_NEXT =
      StyleUtils.joinStyles(
          BaseStyles.BG_SEATTLE_BLUE,
          Styles.TEXT_WHITE,
          Styles.FONT_SEMIBOLD,
          Styles.TEXT_LG,
          Styles.UPPERCASE,
          Styles.ROUNDED_FULL,
          Styles.W_36,
          Styles.FLOAT_RIGHT);

  public static final String PROGRAM_TITLE_HEADING =
      StyleUtils.joinStyles(
          BaseStyles.TEXT_SEATTLE_BLUE, Styles.TEXT_LG, Styles.FONT_BOLD, Styles.MB_4);

  public static final String PROGRAM_CARD =
      StyleUtils.joinStyles(
          Styles.RELATIVE,
          Styles.INLINE_BLOCK,
          Styles.M_4,
          Styles.W_64,
          Styles.H_72,
          Styles.BG_WHITE,
          Styles.ROUNDED_XL,
          Styles.SHADOW_MD,
          Styles.BORDER,
          Styles.BORDER_GRAY_200);
  public static final String BUTTON_PROGRAM_APPLY =
      StyleUtils.joinStyles(
          Styles.BLOCK,
          Styles.UPPERCASE,
          Styles.ROUNDED_FULL,
          Styles.PY_2,
          Styles.PX_6,
          Styles.W_MIN,
          Styles.MX_AUTO,
          Styles.FONT_SEMIBOLD,
          Styles.TEXT_WHITE,
          BaseStyles.BG_SEATTLE_BLUE);
}
