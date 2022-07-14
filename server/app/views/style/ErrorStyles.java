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
}
