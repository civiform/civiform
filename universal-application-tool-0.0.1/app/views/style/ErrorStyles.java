package views.style;

public final class ErrorStyles {
  public static final String NOT_FOUND_TOP_CONTENT =
      StyleUtils.joinStyles(
          BaseStyles.BG_ERROR_GRAY,
          Styles.TEXT_WHITE,
          Styles.TEXT_CENTER,
          Styles.W_FULL,
          Styles.P_6,
          StyleUtils.responsiveSmall(Styles.P_6));
}
