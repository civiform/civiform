package views.style;

/** Styles for applicant pages. */
public final class ApplicantStyles {
  public static final String BODY_BG_COLOR = Styles.BG_CIVIFORM_WHITE;
  public static final String BODY =
      StyleUtils.joinStyles(BODY_BG_COLOR, Styles.H_FULL, Styles.W_FULL);

  public static final String MAIN_APPLICANT_INFO =
      StyleUtils.joinStyles(Styles.W_5_6, Styles.MAX_W_SCREEN_SM, Styles.MX_AUTO, Styles.MY_8);
  public static final String MAIN_PROGRAM_APPLICATION =
      StyleUtils.joinStyles(
          Styles.W_5_6,
          StyleUtils.responsiveSmall(Styles.W_2_3),
          Styles.MX_AUTO,
          Styles.MY_8,
          StyleUtils.responsiveSmall(Styles.MY_12));

  public static final String PROGRAM_INDEX_TOP_CONTENT =
      StyleUtils.joinStyles(
          Styles.BG_SEATTLE_BLUE, Styles.TEXT_WHITE, Styles.TEXT_CENTER, Styles.W_FULL);

  public static final String CIVIFORM_LOGO =
      StyleUtils.joinStyles(
          Styles.TEXT_2XL, Styles.OPACITY_75, StyleUtils.hover(Styles.OPACITY_100));
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
      StyleUtils.joinStyles(Styles.TEXT_SEATTLE_BLUE, Styles.TEXT_LG, Styles.FONT_BOLD);

  public static final String PROGRAM_CARDS_SUBTITLE =
      StyleUtils.joinStyles(Styles.MY_4, Styles.TEXT_LG);
  public static final String PROGRAM_CARDS_CONTAINER_BASE =
      StyleUtils.joinStyles(
          Styles.GRID,
          Styles.GRID_COLS_1,
          Styles.GAP_4,
          Styles.PLACE_ITEMS_CENTER,
          StyleUtils.responsiveSmall(Styles.GRID_COLS_1),
          StyleUtils.responsiveLarge(Styles.GAP_8));
  public static final String PROGRAM_CARD =
      StyleUtils.joinStyles(
          Styles.W_FULL,
          StyleUtils.responsiveSmall(Styles.W_72),
          Styles.H_72,
          Styles.BG_WHITE,
          Styles.ROUNDED_XL,
          Styles.SHADOW_MD,
          Styles.BORDER,
          Styles.BORDER_GRAY_200,
          Styles.FLEX,
          Styles.FLEX_COL,
          Styles.GAP_4);

  public static final String QUESTION_TEXT =
      StyleUtils.joinStyles(Styles.TEXT_BLACK, Styles.TEXT_XL, Styles.FONT_BOLD, Styles.MB_2);
  public static final String QUESTION_HELP_TEXT =
      StyleUtils.joinStyles(Styles.TEXT_BLACK, Styles.TEXT_XL);

  public static final String APPLICATION_NAV_BAR =
      StyleUtils.joinStyles(
          Styles.GAP_4, Styles.FLEX, Styles.FLEX_COL, StyleUtils.responsiveMedium(Styles.FLEX_ROW));

  /**
   * Base styles for buttons in the applicant UI. This is missing a specified text size, so that
   * should be added by other button style constants that use this as a base.
   */
  private static final String BUTTON_BASE =
      StyleUtils.joinStyles(
          Styles.BLOCK, Styles.PY_2, Styles.TEXT_CENTER, Styles.ROUNDED_FULL, Styles.BORDER);

  /** Base styles for buttons with a solid background color. */
  private static final String BUTTON_BASE_SOLID =
      StyleUtils.joinStyles(
          BUTTON_BASE,
          Styles.BG_SEATTLE_BLUE,
          Styles.TEXT_WHITE,
          Styles.ROUNDED_FULL,
          Styles.BORDER_TRANSPARENT,
          StyleUtils.hover(Styles.BG_BLUE_700),
          StyleUtils.disabled(Styles.BG_GRAY_200, Styles.TEXT_GRAY_400));

  /** Base styles for semibold, upper case buttons with a solid background. */
  private static final String BUTTON_BASE_SOLID_UPPERCASE =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID, Styles.UPPERCASE, Styles.FONT_SEMIBOLD, Styles.PX_8);

  /** Base styles for buttons with a transparent background and an outline. */
  private static final String BUTTON_BASE_OUTLINE =
      StyleUtils.joinStyles(
          BUTTON_BASE,
          Styles.BG_TRANSPARENT,
          Styles.TEXT_SEATTLE_BLUE,
          Styles.BORDER,
          Styles.BORDER_SEATTLE_BLUE,
          StyleUtils.hover(Styles.BG_BLUE_100));

  private static final String BUTTON_BASE_OUTLINE_UPPERCASE =
      StyleUtils.joinStyles(
          BUTTON_BASE_OUTLINE, Styles.UPPERCASE, Styles.FONT_SEMIBOLD, Styles.PX_8);

  public static final String BUTTON_SELECT_LANGUAGE =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_UPPERCASE, Styles.TEXT_BASE, Styles.MX_AUTO);
  public static final String BUTTON_PROGRAM_APPLY =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_UPPERCASE, Styles.TEXT_SM, Styles.MX_AUTO);
  public static final String BUTTON_BLOCK_NEXT =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_UPPERCASE, Styles.TEXT_BASE);
  public static final String BUTTON_BLOCK_PREVIOUS =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_UPPERCASE, Styles.TEXT_BASE);
  public static final String BUTTON_REVIEW =
      StyleUtils.joinStyles(BUTTON_BASE_OUTLINE_UPPERCASE, Styles.TEXT_BASE);
  public static final String BUTTON_SUBMIT_APPLICATION =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_UPPERCASE, Styles.TEXT_BASE, Styles.MX_AUTO);
  public static final String BUTTON_ENUMERATOR_ADD_ENTITY =
      StyleUtils.joinStyles(
          BUTTON_BASE_SOLID, Styles.TEXT_BASE, Styles.NORMAL_CASE, Styles.FONT_NORMAL, Styles.PX_4);
  public static final String BUTTON_ENUMERATOR_REMOVE_ENTITY =
      StyleUtils.joinStyles(
          BUTTON_BASE_OUTLINE,
          Styles.TEXT_BASE,
          Styles.NORMAL_CASE,
          Styles.FONT_NORMAL,
          Styles.JUSTIFY_SELF_END,
          Styles.SELF_CENTER);
  public static final String BUTTON_CREATE_ACCOUNT =
      StyleUtils.joinStyles(BUTTON_BASE_SOLID_UPPERCASE, Styles.TEXT_BASE);
  public static final String BUTTON_NOT_RIGHT_NOW =
      StyleUtils.joinStyles(BUTTON_BASE_OUTLINE_UPPERCASE, Styles.TEXT_BASE);
}
