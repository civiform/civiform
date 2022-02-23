package views.style;

/**
 * Constant class containing the names of styles that we have added to tailwind.
 *
 * <p>This file is special - strings in this file, within double quotes, are *not* stripped from the
 * tailwind CSS during production optimization. If you add a string here, run bin/refresh-styles or
 * restart bin/run-dev.
 */
public final class BaseStyles {

  public static final String LINK_TEXT = Styles.TEXT_BLUE_600;
  public static final String LINK_HOVER_TEXT = StyleUtils.hover(Styles.TEXT_BLUE_500);

  public static final String TABLE_CELL_STYLES = StyleUtils.joinStyles(Styles.PX_4, Styles.PY_2);

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // CiviForm color classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  public static final String BG_CIVIFORM_WHITE = "bg-civiform-white";

  public static final String BG_SEATTLE_BLUE = "bg-seattle-blue";
  public static final String TEXT_SEATTLE_BLUE = "text-seattle-blue";
  public static final String BORDER_SEATTLE_BLUE = "border-seattle-blue";

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Form style classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  public static final String FORM_FIELD_MARGIN_BOTTOM = Styles.MB_2;

  public static final String FORM_FIELD_BORDER_COLOR = Styles.BORDER_GRAY_500;
  public static final String FORM_FIELD_ERROR_BORDER_COLOR = Styles.BORDER_RED_600;

  public static final String FORM_LABEL_TEXT_COLOR = Styles.TEXT_GRAY_600;

  public static final String FORM_ERROR_TEXT_COLOR = Styles.TEXT_RED_600;
  public static final String FORM_ERROR_TEXT_XS =
      StyleUtils.joinStyles(BaseStyles.FORM_ERROR_TEXT_COLOR, Styles.TEXT_XS);
  public static final String FORM_ERROR_TEXT_BASE =
      StyleUtils.joinStyles(BaseStyles.FORM_ERROR_TEXT_COLOR, Styles.TEXT_BASE);

  private static final String INPUT_BASE =
      StyleUtils.joinStyles(
          Styles.BLOCK,
          Styles.OUTLINE_NONE,
          Styles.BOX_BORDER,
          Styles.M_AUTO,
          Styles.PX_3,
          Styles.PY_2,
          Styles.BORDER,
          BaseStyles.FORM_FIELD_BORDER_COLOR,
          Styles.ROUNDED_LG,
          Styles.W_FULL,
          Styles.BG_WHITE,
          StyleUtils.focus(BORDER_SEATTLE_BLUE),
          Styles.TEXT_BLACK,
          Styles.TEXT_LG);

  /** For use on `input` elements that are not of type "checkbox" or "radio". */
  public static final String INPUT =
      StyleUtils.joinStyles(INPUT_BASE, Styles.PLACEHOLDER_GRAY_500, Styles.H_12);

  /** For use on `label` elements that label non-checkbox and non-radio `input` elements. */
  public static final String INPUT_LABEL =
      StyleUtils.joinStyles(
          Styles.POINTER_EVENTS_NONE,
          BaseStyles.FORM_LABEL_TEXT_COLOR,
          Styles.TEXT_BASE,
          Styles.PX_1,
          Styles.PY_2);

  /**
   * For use on a `label` that labels a checkbox. The label element should contain the checkbox
   * input element and its label text, e.g., <label><input type="checkbox">This is the label
   * text.</label>
   */
  public static final String CHECKBOX_LABEL =
      StyleUtils.joinStyles(INPUT_BASE, Styles.ALIGN_MIDDLE);
  /** Same as the above but for radio buttons. */
  public static final String RADIO_LABEL = CHECKBOX_LABEL;

  /** For labelling a *group* of checkboxes that are related to the same thing. */
  public static final String CHECKBOX_GROUP_LABEL =
      StyleUtils.joinStyles(BaseStyles.FORM_LABEL_TEXT_COLOR, Styles.TEXT_BASE);

  /** For use on an `input` of type "checkbox". */
  public static final String CHECKBOX =
      StyleUtils.joinStyles(Styles.H_4, Styles.W_4, Styles.MR_4, Styles.ALIGN_MIDDLE);
  /** For use on an `input` of type "radio". */
  public static final String RADIO = CHECKBOX;

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Modal style classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  /** The modal container contains modals, and the glass pane, and covers the whole page. */
  public static final String MODAL_CONTAINER =
      StyleUtils.joinStyles(Styles.HIDDEN, Styles.FIXED, Styles.H_SCREEN, Styles.W_SCREEN);
  /** The modal container for the modal glass pane. */
  public static final String MODAL_GLASS_PANE =
      StyleUtils.joinStyles(
          Styles.FIXED, Styles.H_SCREEN, Styles.W_SCREEN, Styles.BG_GRAY_400, Styles.OPACITY_75);
  /** Generic style for all modals. This should be centered. */
  public static final String MODAL =
      StyleUtils.joinStyles(
          Styles.HIDDEN,
          Styles.ABSOLUTE,
          Styles.LEFT_1_2,
          Styles.TOP_1_2,
          Styles.TRANSFORM,
          Styles._TRANSLATE_X_1_2,
          Styles._TRANSLATE_Y_1_2,
          Styles.ROUNDED_XL,
          Styles.SHADOW_XL,
          Styles.BG_WHITE,
          Styles.MAX_H_SCREEN,
          Styles.OVERFLOW_Y_AUTO);

  public static final String MODAL_HEADER =
      StyleUtils.joinStyles(
          Styles.STICKY,
          Styles.TOP_0,
          Styles.BG_GRAY_200,
          Styles.P_2,
          Styles.FLEX,
          Styles.GAP_4,
          Styles.PLACE_ITEMS_CENTER);
  /** Generic style for for the button for the modal. */
  public static final String MODAL_BUTTON =
      StyleUtils.joinStyles(
          Styles.BLOCK,
          Styles.PY_2,
          Styles.TEXT_CENTER,
          Styles.ROUNDED_FULL,
          BaseStyles.BG_SEATTLE_BLUE,
          StyleUtils.hover(Styles.BG_BLUE_700),
          Styles.TEXT_WHITE,
          Styles.ROUNDED_FULL);
  /** Generic styles for the button to close the modal. This is shared across all modals. */
  public static final String MODAL_CLOSE_BUTTON =
      StyleUtils.joinStyles(
          Styles.FONT_BOLD,
          Styles.CURSOR_POINTER,
          Styles.OPACITY_60,
          Styles.PX_2,
          StyleUtils.hover(Styles.OPACITY_100));
  /**
   * Simple styling for the div that holds the custom modal content. Should just have decent margins
   * and sizing.
   */
  public static final String MODAL_CONTENT = StyleUtils.joinStyles(Styles.MY_4);

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Login style classes
  /////////////////////////////////////////////////////////////////////////////////////////////////
  public static final String LOGIN_PAGE =
      StyleUtils.joinStyles(
          Styles.ABSOLUTE,
          Styles.LEFT_1_2,
          Styles.TOP_1_2,
          Styles.TRANSFORM,
          Styles._TRANSLATE_X_1_2,
          Styles._TRANSLATE_Y_1_2,
          Styles.BORDER,
          Styles.BORDER_GRAY_200,
          Styles.ROUNDED_LG,
          Styles.SHADOW_XL,
          Styles.BG_WHITE,
          Styles.FLEX,
          Styles.FLEX_COL,
          Styles.GAP_2,
          Styles.PLACE_ITEMS_CENTER);

  private static final String LOGIN_REDIRECT_BUTTON_BASE =
      StyleUtils.joinStyles(Styles.ROUNDED_3XL, Styles.UPPERCASE);

  public static final String LOGIN_REDIRECT_BUTTON =
      StyleUtils.joinStyles(
          LOGIN_REDIRECT_BUTTON_BASE,
          Styles.BG_BLUE_800,
          Styles.TEXT_WHITE,
          Styles.W_3_4,
          StyleUtils.responsiveMedium(Styles.W_1_3));

  public static final String LOGIN_REDIRECT_BUTTON_SECONDARY =
      StyleUtils.joinStyles(
          LOGIN_REDIRECT_BUTTON_BASE,
          Styles.BORDER,
          Styles.BORDER_BLUE_800,
          Styles.TEXT_BLUE_800,
          Styles.TEXT_BASE,
          Styles.BG_WHITE,
          StyleUtils.hover(Styles.BG_BLUE_100, Styles.OPACITY_90));

  public static final String ADMIN_LOGIN =
      StyleUtils.joinStyles(
          Styles.BG_TRANSPARENT,
          Styles.TEXT_BLACK,
          Styles.UNDERLINE,
          Styles.FONT_BOLD,
          StyleUtils.hover(Styles.BG_GRAY_200, Styles.OPACITY_90));
}
