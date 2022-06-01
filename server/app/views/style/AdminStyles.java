package views.style;

/** Styles for admin pages. */
public class AdminStyles {

  public static final String LANGUAGE_LINK_SELECTED =
      StyleUtils.joinStyles(
          ReferenceClasses.ADMIN_LANGUAGE_LINK,
          Styles.M_2,
          Styles.BORDER_BLUE_400,
          Styles.BORDER_B_2);

  public static final String LANGUAGE_LINK_NOT_SELECTED =
      StyleUtils.joinStyles(ReferenceClasses.ADMIN_LANGUAGE_LINK, Styles.M_2);

  public static final String ADMIN_NAV_BAR =
      StyleUtils.joinStyles(
          Styles.ABSOLUTE,
          Styles.BG_CONTAIN,
          Styles.BG_GRAY_700,
          Styles.H_7,
          Styles.LEFT_5,
          Styles.M_1,
          Styles.OPACITY_75,
          Styles.TEXT_CENTER,
          Styles.TEXT_LG,
          Styles.TEXT_WHITE,
          Styles.TOP_2,
          Styles.W_7,
          Styles.ROUNDED);

  public static final String BODY_GRADIENT_STYLE =
      StyleUtils.joinStyles(
          Styles.BG_GRADIENT_TO_R, Styles.FROM_GRAY_100, Styles.VIA_WHITE, Styles.TO_GRAY_100);

  public static final String NAV_BACKGROUND_COLOR = Styles.BG_WHITE;
  public static final String NAV_TEXT_COLOR = Styles.TEXT_GRAY_700;
  public static final String NAV_HEIGHT = Styles.H_12;
  public static final String NAV_FIXED =
      StyleUtils.joinStyles(Styles.FIXED, Styles.TOP_0, Styles.W_SCREEN);
  public static final String NAV_PADDING = StyleUtils.joinStyles(Styles.PX_4, Styles.PY_3);
  public static final String NAV_STYLES =
      StyleUtils.joinStyles(
          Styles.SHADOW, NAV_BACKGROUND_COLOR, NAV_TEXT_COLOR, NAV_HEIGHT, NAV_FIXED, NAV_PADDING);

  /** Invisible buttons covering an area that are used for form submit. */
  public static final String CLICK_TARGET_BUTTON =
      StyleUtils.joinStyles(
          Styles.ABSOLUTE,
          Styles.H_FULL,
          Styles.LEFT_0,
          Styles.OPACITY_0,
          Styles.TOP_0,
          Styles.W_FULL);

  public static final String MOVE_BLOCK_BUTTON =
      StyleUtils.joinStyles(
          Styles.BG_TRANSPARENT,
          Styles.P_0,
          Styles.W_6,
          Styles.TEXT_CENTER,
          Styles.TEXT_GRAY_500,
          StyleUtils.hover(Styles.BG_GRAY_200, Styles.TEXT_GRAY_900));

  public static final String BODY =
      StyleUtils.joinStyles(
          BODY_GRADIENT_STYLE,
          Styles.BOX_BORDER,
          Styles.H_SCREEN,
          Styles.W_SCREEN,
          Styles.OVERFLOW_HIDDEN,
          Styles.FLEX);

  public static final String MAIN_CENTERED =
      StyleUtils.joinStyles(Styles.PX_2, Styles.MAX_W_SCREEN_2XL, Styles.MX_AUTO);

  public static final String MAIN_FULL = StyleUtils.joinStyles(Styles.FLEX, Styles.FLEX_ROW);

  public static final String MAIN =
      StyleUtils.joinStyles(
          Styles.BG_WHITE,
          Styles.BORDER,
          Styles.BORDER_GRAY_200,
          Styles.MT_12,
          Styles.OVERFLOW_Y_AUTO,
          Styles.SHADOW_LG,
          Styles.W_SCREEN);

  public static final String BUTTON_QUESTION_PREDICATE =
      StyleUtils.joinStyles(
          Styles.W_FULL,
          Styles.PX_4,
          Styles.PY_2,
          Styles.BORDER,
          Styles.BORDER_GRAY_200,
          Styles.TEXT_BLACK,
          Styles.TEXT_LEFT,
          Styles.FONT_NORMAL,
          Styles.BG_WHITE,
          StyleUtils.hover(Styles.TEXT_GRAY_800, Styles.BG_GRAY_100));

  private static final String BASE_BUTTON_STYLES =
      StyleUtils.joinStyles(Styles.FLEX, Styles.ITEMS_CENTER, Styles.FONT_MEDIUM);

  public static final String PRIMARY_BUTTON_STYLES =
      StyleUtils.joinStyles(
          BASE_BUTTON_STYLES, Styles.ROUNDED_FULL, Styles.BG_SEATTLE_BLUE, Styles.TEXT_WHITE);

  public static final String SECONDARY_BUTTON_STYLES =
      StyleUtils.joinStyles(
          BASE_BUTTON_STYLES,
          Styles.ROUNDED_FULL,
          Styles.BORDER,
          Styles.BORDER_SEATTLE_BLUE,
          Styles.BG_WHITE,
          Styles.TEXT_SEATTLE_BLUE,
          StyleUtils.hover(Styles.BG_GRAY_200));

  public static final String TERTIARY_BUTTON_STYLES =
      StyleUtils.joinStyles(
          BASE_BUTTON_STYLES,
          Styles.BORDER_NONE,
          Styles.ROUNDED,
          Styles.BG_TRANSPARENT,
          Styles.TEXT_BLACK,
          StyleUtils.hover(Styles.BG_GRAY_200));
}
