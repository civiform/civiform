package views.style;

public final class ApplicantStyles {
  public static final String BODY_BG_COLOR = BaseStyles.BG_CIVIFORM_WHITE;
  public static final String BODY =
      StyleUtils.joinStyles(BODY_BG_COLOR, Styles.H_FULL, Styles.W_FULL);

  public static final String MAIN_PROGRAM_APPLICATION =
      StyleUtils.joinStyles(
          Styles.W_5_6,
          StyleUtils.responsiveSmall(Styles.W_2_3),
          Styles.MX_AUTO,
          Styles.MY_8,
          StyleUtils.responsiveSmall(Styles.MY_12));

  public static final String PROGRAM_INDEX_TOP_CONTENT =
      StyleUtils.joinStyles(
          BaseStyles.BG_SEATTLE_BLUE,
          Styles.TEXT_WHITE,
          Styles.TEXT_CENTER,
          Styles.W_FULL,
          Styles.P_6,
          StyleUtils.responsiveSmall(Styles.P_10));

  public static final String CIVIFORM_LOGO = StyleUtils.joinStyles(Styles.TEXT_2XL);
  public static final String LINK_LOGOUT =
      StyleUtils.joinStyles(
          Styles.TEXT_BASE,
          Styles.FONT_BOLD,
          Styles.OPACITY_75,
          StyleUtils.hover(Styles.OPACITY_100));

  public static final String H1_PROGRAM_APPLICATION =
      StyleUtils.joinStyles(
          Styles.TEXT_3XL, Styles.TEXT_BLACK, Styles.FONT_BOLD, Styles.MT_8, Styles.MB_4);
  public static final String H2_PROGRAM_TITLE =
      StyleUtils.joinStyles(BaseStyles.TEXT_SEATTLE_BLUE, Styles.TEXT_LG, Styles.FONT_BOLD);

  public static final String PROGRAM_CARDS_CONTAINER =
      StyleUtils.joinStyles(Styles.FLEX, Styles.FLEX_WRAP, Styles.GAP_4);
  public static final String PROGRAM_CARD =
      StyleUtils.joinStyles(
          Styles.RELATIVE,
          Styles.INLINE_BLOCK,
          Styles.W_FULL,
          StyleUtils.responsiveSmall(Styles.W_72),
          Styles.H_72,
          Styles.BG_WHITE,
          Styles.ROUNDED_XL,
          Styles.SHADOW_MD,
          Styles.BORDER,
          Styles.BORDER_GRAY_200);

  public static final String QUESTION_TEXT =
      StyleUtils.joinStyles(Styles.TEXT_BLACK, Styles.TEXT_XL, Styles.FONT_BOLD, Styles.MB_2);
  public static final String QUESTION_HELP_TEXT =
      StyleUtils.joinStyles(Styles.TEXT_BLACK, Styles.TEXT_XL);

  /**
   * Base styles for buttons in the applicant UI. This is missing a specified text size, so that
   * should be added by other button style constants that use this as a base.
   */
  private static final String BUTTON_BASE =
      StyleUtils.joinStyles(
          BaseStyles.BG_SEATTLE_BLUE,
          StyleUtils.hover(Styles.BG_BLUE_700),
          Styles.BLOCK,
          Styles.W_MIN,
          Styles.PX_8,
          Styles.PY_2,
          Styles.TEXT_CENTER,
          Styles.TEXT_WHITE,
          Styles.FONT_SEMIBOLD,
          Styles.UPPERCASE,
          Styles.ROUNDED_FULL);

  public static final String BUTTON_PROGRAM_APPLY =
      StyleUtils.joinStyles(BUTTON_BASE, Styles.TEXT_SM, Styles.MX_AUTO);
  public static final String BUTTON_BLOCK_NEXT =
      StyleUtils.joinStyles(BUTTON_BASE, Styles.TEXT_BASE, Styles.MR_0, Styles.ML_AUTO);
  public static final String BUTTON_SUBMIT_APPLICATION =
      StyleUtils.joinStyles(BUTTON_BASE, Styles.TEXT_BASE, Styles.MX_AUTO);
}
