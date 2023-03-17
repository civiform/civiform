package views.style;

/**
 * Constant class containing the names of styles that we have added to tailwind.
 *
 * <p>This file is special - strings in this file, within double quotes, are *not* stripped from the
 * tailwind CSS during production optimization. If you add a string here, run bin/refresh-styles or
 * restart bin/run-dev.
 */
public final class BaseStyles {

  public static final String LINK_TEXT = "text-blue-600";
  public static final String LINK_HOVER_TEXT = StyleUtils.hover("text-blue-500");

  public static final String TABLE_CELL_STYLES = StyleUtils.joinStyles("px-4", "py-2");

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // CiviForm color classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  public static final String BG_CIVIFORM_WHITE = "bg-civiform-white";

  public static final String BG_SEATTLE_BLUE = "bg-seattle-blue";
  public static final String TEXT_SEATTLE_BLUE = "text-seattle-blue";
  public static final String BORDER_SEATTLE_BLUE = "border-seattle-blue";

  public static final String TEXT_CIVIFORM_GREEN = "text-civiform-green";
  public static final String BG_CIVIFORM_GREEN_LIGHT = "bg-civiform-green-light";
  public static final String TEXT_CIVIFORM_PURPLE = "text-civiform-purple";
  public static final String BG_CIVIFORM_PURPLE_LIGHT = "bg-civiform-purple-light";

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Form style classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  public static final String FORM_FIELD_MARGIN_BOTTOM = "mb-2";

  public static final String FORM_FIELD_BORDER_COLOR = "border-gray-500";
  public static final String FORM_FIELD_ERROR_BORDER_COLOR = "border-red-600";

  public static final String FORM_LABEL_TEXT_COLOR = "text-gray-600";

  public static final String FORM_ERROR_TEXT_COLOR = "text-red-600";
  public static final String FORM_ERROR_TEXT_XS =
      StyleUtils.joinStyles(BaseStyles.FORM_ERROR_TEXT_COLOR, "text-xs");
  public static final String FORM_ERROR_TEXT_BASE =
      StyleUtils.joinStyles(BaseStyles.FORM_ERROR_TEXT_COLOR, "text-base");

  public static final String FORM_FIELD =
      StyleUtils.joinStyles("px-3", "bg-white", "text-black", "text-lg");

  private static final String INPUT_BASE =
      StyleUtils.joinStyles(
          FORM_FIELD,
          "py-2",
          "block",
          "outline-none",
          "box-border",
          "m-auto",
          "border",
          BaseStyles.FORM_FIELD_BORDER_COLOR,
          "rounded-lg",
          "w-full",
          StyleUtils.focus(BORDER_SEATTLE_BLUE));

  /** For use on `input` elements that are not of type "checkbox" or "radio". */
  public static final String INPUT = StyleUtils.joinStyles(INPUT_BASE, "placeholder-gray-500");

  public static final String INPUT_WITH_ERROR =
      StyleUtils.joinStyles(
          StyleUtils.removeStyles(INPUT, BaseStyles.FORM_FIELD_BORDER_COLOR),
          FORM_FIELD_ERROR_BORDER_COLOR);

  /** For use on `label` elements that label non-checkbox and non-radio `input` elements. */
  public static final String INPUT_LABEL =
      StyleUtils.joinStyles(
          "pointer-events-none", BaseStyles.FORM_LABEL_TEXT_COLOR, "text-base", "px-1", "py-2");

  /**
   * For use on a `label` that labels a checkbox. The label element should contain the checkbox
   * input element and its label text, e.g., <label><input type="checkbox">This is the label
   * text.</label>
   */
  public static final String CHECKBOX_LABEL = StyleUtils.joinStyles(INPUT_BASE, "align-middle");
  /** Same as the above but for radio buttons. */
  public static final String RADIO_LABEL = CHECKBOX_LABEL;

  public static final String RADIO_LABEL_SELECTED =
      StyleUtils.removeStyles(RADIO_LABEL, FORM_FIELD_BORDER_COLOR);

  /** For labelling a *group* of checkboxes that are related to the same thing. */
  public static final String CHECKBOX_GROUP_LABEL =
      StyleUtils.joinStyles(BaseStyles.FORM_LABEL_TEXT_COLOR, "text-base");

  /** For use on an `input` of type "checkbox". */
  public static final String CHECKBOX = StyleUtils.joinStyles("h-4", "w-4", "mr-4", "align-middle");
  /** For use on an `input` of type "radio". */
  public static final String RADIO = CHECKBOX;

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Modal style classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  /** The modal container contains modals, and the glass pane, and covers the whole page. */
  public static final String MODAL_CONTAINER =
      StyleUtils.joinStyles("hidden", "fixed", "h-screen", "w-screen", "z-20");
  /** The modal container for the modal glass pane. */
  public static final String MODAL_GLASS_PANE =
      StyleUtils.joinStyles("fixed", "h-screen", "w-screen", "bg-gray-400", "opacity-75");
  /** Generic style for all modals. This should be centered. */
  public static final String MODAL =
      StyleUtils.joinStyles(
          "hidden",
          "absolute",
          "left-1/2",
          "top-1/2",
          "transform",
          "-translate-x-1/2",
          "-translate-y-1/2",
          "rounded-xl",
          "shadow-xl",
          "bg-white",
          "max-h-screen",
          "overflow-y-auto");

  public static final String MODAL_HEADER =
      StyleUtils.joinStyles(
          "sticky", "top-0", "bg-gray-200", "p-2", "flex", "gap-4", "place-items-center");
  /** Generic style for for the button for the modal. */
  public static final String MODAL_BUTTON =
      StyleUtils.joinStyles(
          "block",
          "py-2",
          "text-center",
          "rounded-full",
          BaseStyles.BG_SEATTLE_BLUE,
          StyleUtils.hover("bg-blue-700"),
          "text-white",
          "rounded-full");
  /** Generic styles for the button to close the modal. This is shared across all modals. */
  public static final String MODAL_CLOSE_BUTTON =
      StyleUtils.joinStyles(
          "font-bold",
          "cursor-pointer",
          "opacity-60",
          "w-7",
          "h-7",
          StyleUtils.hover("opacity-100"));
  /**
   * Simple styling for the div that holds the custom modal content. Should just have decent margins
   * and sizing.
   */
  public static final String MODAL_CONTENT = StyleUtils.joinStyles("my-4");

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Login style classes
  /////////////////////////////////////////////////////////////////////////////////////////////////
  public static final String LOGIN_PAGE =
      StyleUtils.joinStyles(
          "absolute",
          "left-1/2",
          "top-1/2",
          "transform",
          "-translate-x-1/2",
          "-translate-y-1/2",
          "border",
          "border-gray-200",
          "rounded-lg",
          "shadow-xl",
          "bg-white",
          "flex",
          "flex-col",
          "gap-2",
          "place-items-center");

  private static final String LOGIN_REDIRECT_BUTTON_BASE = StyleUtils.joinStyles("rounded-3xl");

  public static final String LOGIN_REDIRECT_BUTTON_OLD =
      StyleUtils.joinStyles(
          LOGIN_REDIRECT_BUTTON_BASE,
          "bg-blue-800",
          "text-white",
          "w-3/4",
          StyleUtils.responsiveMedium("w-1/3"));

  public static final String LOGIN_REDIRECT_BUTTON_NEW =
      StyleUtils.joinStyles(
          LOGIN_REDIRECT_BUTTON_BASE,
          "bg-blue-800",
          "text-white",
          "w-3/4",
          StyleUtils.responsiveMedium("w-2/5"));

  public static final String LOGIN_REDIRECT_BUTTON_SECONDARY =
      StyleUtils.joinStyles(
          LOGIN_REDIRECT_BUTTON_BASE,
          "border",
          "border-blue-800",
          "text-blue-800",
          "text-base",
          "bg-white",
          StyleUtils.hover("bg-blue-100/90"));

  public static final String LOGIN_REDIRECT_BUTTON_SECONDARY_NEW =
      StyleUtils.joinStyles(
          LOGIN_REDIRECT_BUTTON_BASE,
          "border",
          "border-blue-800",
          "text-blue-800",
          "text-base",
          "bg-white",
          StyleUtils.responsiveMedium("w-2/5"));

  public static final String ADMIN_LOGIN =
      StyleUtils.joinStyles(
          "bg-transparent",
          "text-black",
          "underline",
          "font-bold",
          StyleUtils.hover("bg-gray-200", "opacity-90"));

  public static final String ADMIN_LOGIN_NEW =
      StyleUtils.joinStyles(
          "bg-transparent",
          "text-blue-700",
          "font-semibold",
          StyleUtils.hover("bg-gray-200", "opacity-90"));

  public static final String GUEST_LOGIN =
      StyleUtils.joinStyles(
          "bg-transparent", "text-blue-800", StyleUtils.hover("bg-gray-200", "opacity-90"));
}
