package views;

public final class BaseStyles {

  public static final String FIELD_BACKGROUND_COLOR = Styles.BG_GRAY_50;
  public static final String FIELD_BORDER_COLOR = Styles.BORDER_GRAY_500;

  public static final String LABEL_BACKGROUND_COLOR = Styles.BG_TRANSPARENT;
  public static final String LABEL_TEXT_COLOR = Styles.TEXT_GRAY_600;

  public static final String NAV_BACKGROUND_COLOR = Styles.BG_GRAY_800;
  public static final String NAV_TEXT_COLOR = Styles.TEXT_GRAY_200;
  public static final String NAV_HEIGHT = Styles.H_12;
  public static final String NAV_FIXED =
      String.join(" ", Styles.FIXED, Styles.TOP_0, Styles.W_SCREEN);
  public static final String NAV_PADDING = String.join(" ", Styles.PX_4, Styles.PY_3);
  public static final String NAV_STYLES =
      String.join(" ", NAV_BACKGROUND_COLOR, NAV_TEXT_COLOR, NAV_HEIGHT, NAV_FIXED, NAV_PADDING);
}
