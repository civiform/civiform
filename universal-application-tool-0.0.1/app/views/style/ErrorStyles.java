package views.style;

public final class ErrorStyles {
  public static final String H1_NOT_FOUND =
      StyleUtils.joinStyles(
          Styles.TEXT_2XL,
          Styles.TEXT_BLACK,
          Styles.FONT_BOLD,
          Styles.TEXT_CENTER,
          Styles.TEXT_GRAY_800,
          Styles.MT_14,
          Styles.MB_4,
          StyleUtils.responsiveSmall(Styles.MT_28, Styles.TEXT_3XL));

  public static final String P_DESCRIPTION =
      StyleUtils.joinStyles(
          Styles.TEXT_XL,
          Styles.TEXT_BASE,
          Styles.TEXT_BLACK,
          Styles.TEXT_CENTER,
          Styles.TEXT_GRAY_700,
          Styles.MB_6,
          StyleUtils.responsiveSmall(Styles.MT_10));

  public static final String P_MOBILE_INLINE =
      StyleUtils.joinStyles(Styles.INLINE, StyleUtils.responsiveSmall(Styles.BLOCK));

  public static final String PHOTO = StyleUtils.joinStyles(Styles.M_AUTO, Styles.OPACITY_80);

  public static final String P_IMG_FOOTER =
      StyleUtils.joinStyles(Styles.TEXT_BASE, Styles.TEXT_BLACK, Styles.MT_3);

  public static final String NOT_FOUND_TOP_CONTENT =
      StyleUtils.joinStyles(
          BaseStyles.BG_ERROR_GRAY,
          Styles.TEXT_WHITE,
          Styles.TEXT_CENTER,
          Styles.TEXT_GRAY_700,
          Styles.W_FULL,
          Styles.P_6,
          StyleUtils.responsiveSmall(Styles.P_6, Styles.TEXT_LEFT));
}
