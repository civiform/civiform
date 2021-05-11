package views.style;

public final class BaseStyles {
  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Admin style classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

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

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Form style classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  public static final String APPLICANT_BG_COLOR = "bg-beige";

  public static final String FORM_FIELD_MARGIN_BOTTOM = Styles.MB_4;

  public static final String FORM_FIELD_BORDER_COLOR = Styles.BORDER_GRAY_300;
  public static final String FORM_FIELD_ERROR_BORDER_COLOR = Styles.BORDER_RED_600;

  public static final String FORM_LABEL_TEXT_COLOR = Styles.TEXT_GRAY_600;

  public static final String FORM_ERROR_TEXT_COLOR = Styles.TEXT_RED_600;
  public static final String FORM_ERROR_TEXT =
      StyleUtils.joinStyles(BaseStyles.FORM_ERROR_TEXT_COLOR, Styles.TEXT_XS);

  /** For use on `label` elements that label non-checkbox and non-radio `input` elements. */
  public static final String INPUT_LABEL =
      StyleUtils.joinStyles(
          Styles.POINTER_EVENTS_NONE,
          BaseStyles.FORM_LABEL_TEXT_COLOR,
          Styles.TEXT_BASE,
          Styles.PX_1,
          Styles.PY_2);

  /** For use on `input` elements that are not of type "checkbox" or "radio". */
  public static final String INPUT =
      StyleUtils.joinStyles(
          Styles.BLOCK,
          Styles.OUTLINE_NONE,
          Styles.M_AUTO,
          Styles.PX_3,
          Styles.PY_2,
          Styles.BORDER,
          BaseStyles.FORM_FIELD_BORDER_COLOR,
          Styles.ROUNDED_LG,
          Styles.W_FULL,
          Styles.TEXT_LG,
          Styles.PLACEHOLDER_GRAY_400,
          StyleUtils.focus(Styles.BORDER_BLUE_500));

  /** For use on an `input` of type "checkbox". */
  public static final String CHECKBOX =
      StyleUtils.joinStyles(Styles.H_4, Styles.W_4, Styles.MR_4, Styles.ALIGN_MIDDLE);

  public static final String CHECKBOX_WITH_NO_LABEL =
      StyleUtils.joinStyles(Styles.H_4, Styles.W_4, Styles.ALIGN_MIDDLE);
  /** For use on an `input` of type "radio". */
  public static final String RADIO = CHECKBOX;
  /** For use on a `label` that labels a checkbox. */
  public static final String CHECKBOX_LABEL =
      StyleUtils.joinStyles(
          Styles.TEXT_BASE, Styles.ALIGN_MIDDLE, BaseStyles.FORM_LABEL_TEXT_COLOR);
  /** For use on a `label` that labels a radio button. */
  public static final String RADIO_LABEL = CHECKBOX_LABEL;

  /** For use on a `div` that contains an `input` element of type "checkbox". */
  public static final String CHECKBOX_OPTION_CONTAINER =
      StyleUtils.joinStyles(INPUT, Styles.BG_WHITE);
  /** For use on a `div` that contains an `input` element of type "radio". */
  public static final String RADIO_OPTION_CONTAINER = CHECKBOX_OPTION_CONTAINER;

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Common style classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  public static final String LINK_TEXT = Styles.TEXT_BLUE_400;
  public static final String LINK_HOVER_TEXT = StyleUtils.hover(Styles.TEXT_BLUE_500);

  public static final String TABLE_CELL_STYLES = StyleUtils.joinStyles(Styles.PX_4, Styles.PY_2);
}
