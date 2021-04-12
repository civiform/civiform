package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

import com.google.common.base.Strings;
import j2html.tags.ContainerTag;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public class ToastMessage {

  enum ToastType {
    ALERT,
    ERROR,
    SUCCESS,
    WARNING
  }

  private ToastType type = ToastType.ALERT;

  private String id = "";
  private String message = "";

  private String CORE_TOAST_CLASSES =
      StyleUtils.joinStyles(
          ReferenceClasses.TOAST_MESSAGE,
          Styles.BG_OPACITY_90,
          Styles.DURATION_300,
          Styles.FLEX,
          Styles.FLEX_ROW,
          Styles.MAX_W_MD,
          // Styles.OPACITY_0,
          Styles.PX_2,
          Styles.PY_2,
          Styles.MY_3,
          Styles.RELATIVE,
          Styles.ROUNDED_SM,
          Styles.SHADOW_LG,
          Styles.TEXT_GRAY_700,
          Styles.TRANSITION_OPACITY,
          Styles.TRANSFORM);

  private String ALERT_CLASSES = StyleUtils.joinStyles(Styles.BG_GRAY_200, Styles.BORDER_GRAY_300);
  
  private String ERROR_CLASSES = StyleUtils.joinStyles(Styles.BG_RED_400, Styles.BORDER_RED_500);

  private String SUCCESS_CLASSES =
      StyleUtils.joinStyles(Styles.BG_GREEN_200, Styles.BORDER_GREEN_300);

  private String WARNING_CLASSES =
      StyleUtils.joinStyles(Styles.BG_YELLOW_200, Styles.BORDER_YELLOW_300);

  /** Default duration is 3 seconds. */
  private int duration = 3000;

  private boolean canDismiss = false;

  /** If true this message will not be shown if a user has already seen and dismissed it. */
  private boolean canIgnore = false;
  
  private static final String BANNER_TEXT = "Do not enter actual or personal data in this demo site";

  public static ContainerTag toastContainer(String... toastMessages) {
    ContainerTag toastContainer = div().withId("toast-container")
      .withClasses("absolute transform -translate-x-1/2 left-1/2");

    // Add privacy banner. (Remove before launch.)
    ToastMessage privacyBanner = 
        ToastMessage.error(ToastMessage.BANNER_TEXT)
        .setDismissible(true).setIgnorable(true);
    toastContainer.with(privacyBanner.getContainer());

    for (String message: toastMessages) {
      ToastMessage toast = ToastMessage.warning(message);
      toastContainer.with(toast.getContainer());
    }

    return toastContainer;
  }

  public static ToastMessage alert(String message) {
    return new ToastMessage().setType(ToastType.ALERT).setMessage(message);
  }

  public static ToastMessage error(String message) {
    return new ToastMessage().setType(ToastType.ERROR).setMessage(message);
  }

  public static ToastMessage success(String message) {
    return new ToastMessage().setType(ToastType.SUCCESS).setMessage(message);
  }

  public static ToastMessage warning(String message) {
    return new ToastMessage().setType(ToastType.WARNING).setMessage(message);
  }

  public ToastMessage setDismissible(boolean canDismiss) {
    this.canDismiss = canDismiss;
    return this;
  }

  public ToastMessage setIgnorable(boolean canIgnore) {
    this.canIgnore = canIgnore;
    return this;
  }

  public ToastMessage setDuration(int duration) {
    this.duration = duration;
    return this;
  }

  public ToastMessage setId(String id) {
    this.id = id;
    return this;
  }

  public ToastMessage setMessage(String message) {
    this.message = message;
    return this;
  }

  public ToastMessage setType(ToastType type) {
    this.type = type;
    return this;
  }

  public ContainerTag getContainer() {
    ContainerTag wrappedWarningSvg = div().withClasses(Styles.FLEX_NONE, Styles.PR_2);
    ContainerTag messageSpan = span(message);

    String styles = CORE_TOAST_CLASSES;
    switch (this.type) {
      case ALERT:
        styles = StyleUtils.joinStyles(styles, ALERT_CLASSES);
        wrappedWarningSvg.with(
            Icons.svg(Icons.INFO_SVG_PATH, 20)
                .attr("fill-rule", "evenodd")                
                .withClasses(Styles.INLINE_BLOCK, Styles.H_6, Styles.W_6));
        break;
      case ERROR: 
        styles = StyleUtils.joinStyles(styles, ERROR_CLASSES);
        wrappedWarningSvg.with(
            Icons.svg(Icons.ERROR_SVG_PATH, 20)
            .attr("fill-rule", "evenodd")
                .withClasses(Styles.INLINE_BLOCK, Styles.H_6, Styles.W_6));
        break;
      case SUCCESS: 
        styles = StyleUtils.joinStyles(styles, SUCCESS_CLASSES);
        wrappedWarningSvg.with(
            Icons.svg(Icons.CHECK_SVG_PATH, 20)
                .attr("fill", "none")
                .attr("stroke-width", "2")
                .withClasses(Styles.INLINE_BLOCK, Styles.H_6, Styles.W_6));
        break;
      case WARNING:
        styles = StyleUtils.joinStyles(styles, WARNING_CLASSES);
        wrappedWarningSvg.with(
            Icons.svg(Icons.WARNING_SVG_PATH, 20)
                .attr("fill-rule", "evenodd")
                .withClasses(Styles.INLINE_BLOCK, Styles.H_6, Styles.W_6));
        break;
    }

    ContainerTag ret =
        div(wrappedWarningSvg, messageSpan)
            .withCondId(Strings.isNullOrEmpty(this.id), this.id)
            .condAttr(this.duration != -1, "duration", this.duration + "")
            .withClasses(styles);

    if (canDismiss || canIgnore) {
      ContainerTag dismissButton =
          div("x")
              .withCondId(Strings.isNullOrEmpty(this.id), this.id + "-dismiss")
              .withClasses(
                  Styles.FONT_BOLD,
                  Styles.PL_6,
                  Styles.OPACITY_40,
                  Styles.CURSOR_POINTER,
                  StyleUtils.hover(Styles.OPACITY_100));
      ret.with(dismissButton);
    }

    return ret;
  }
}
