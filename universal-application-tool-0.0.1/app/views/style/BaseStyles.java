package views.style;

public final class BaseStyles {

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

  /** Invisible buttons covering an area that are used for form submit. */
  public static final String CLICK_TARGET_BUTTON =
      StyleUtils.joinStyles(
          Styles.ABSOLUTE,
          Styles.H_FULL,
          Styles.LEFT_0,
          Styles.OPACITY_0,
          Styles.TOP_0,
          Styles.W_FULL);

  public static final String FIELD_BACKGROUND_COLOR = Styles.BG_GRAY_50;
  public static final String FIELD_BORDER_COLOR = Styles.BORDER_GRAY_500;
  public static final String FIELD_ERROR_BORDER_COLOR = Styles.BORDER_RED_600;

  public static final String LABEL_BACKGROUND_COLOR = Styles.BG_TRANSPARENT;
  public static final String LABEL_TEXT_COLOR = Styles.TEXT_GRAY_600;

  public static final String VALIDATION_ERROR_TEXT_COLOR = Styles.TEXT_RED_600;

  public static final String ERROR_TEXT = StyleUtils.joinStyles(BaseStyles.VALIDATION_ERROR_TEXT_COLOR, Styles.TEXT_SM);

  public static final String LINK_TEXT = Styles.TEXT_BLUE_400;
  public static final String LINK_HOVER_TEXT = StyleUtils.hover(Styles.TEXT_BLUE_500);

  public static final String NAV_BACKGROUND_COLOR = Styles.BG_WHITE;
  public static final String NAV_TEXT_COLOR = Styles.TEXT_GRAY_700;
  public static final String NAV_HEIGHT = Styles.H_12;
  public static final String NAV_FIXED =
      StyleUtils.joinStyles(Styles.FIXED, Styles.TOP_0, Styles.W_SCREEN);
  public static final String NAV_PADDING = StyleUtils.joinStyles(Styles.PX_4, Styles.PY_3);
  public static final String NAV_STYLES =
      StyleUtils.joinStyles(
          Styles.SHADOW, NAV_BACKGROUND_COLOR, NAV_TEXT_COLOR, NAV_HEIGHT, NAV_FIXED, NAV_PADDING);

  public static final String TABLE_CELL_STYLES = StyleUtils.joinStyles(Styles.PX_4, Styles.PY_2);
}
