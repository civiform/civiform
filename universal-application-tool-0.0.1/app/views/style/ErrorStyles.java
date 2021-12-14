package views.style;

public final class ErrorStyles {
  public static final String H1_NOT_FOUND =
      StyleUtils.joinStyles(
          Styles.TEXT_3XL, Styles.TEXT_BLACK, Styles.FONT_BOLD, Styles.MT_16, StyleUtils.responsiveMedium(Styles.MT_28), Styles.MB_8);

  public static final String P_DESCRIPTION =
      StyleUtils.joinStyles(
          Styles.TEXT_XL, Styles.TEXT_BASE, Styles.TEXT_BLACK, Styles.MB_6, StyleUtils.responsiveMedium(Styles.MT_16));

  public static final String P_IMG_FOOTER =
      StyleUtils.joinStyles(
          Styles.TEXT_BASE, Styles.TEXT_BLACK, Styles.MT_3);

  public static final String NOT_FOUND_TOP_CONTENT =
      StyleUtils.joinStyles(
          BaseStyles.BG_ERROR_GRAY,
          Styles.TEXT_WHITE,
          Styles.TEXT_CENTER,
          Styles.W_FULL,
          Styles.P_6,
          StyleUtils.responsiveSmall(Styles.P_6));
}
