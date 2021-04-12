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
    WARNING
  }

  private ToastType type = ToastType.ALERT;

  private String id = "";
  private String message = "";

  private String CORE_TOAST_CLASSES =
      StyleUtils.joinStyles(
          ReferenceClasses.TOAST_MESSAGE,
          Styles.ABSOLUTE,
          Styles.BG_OPACITY_90,
          Styles.DURATION_300,
          Styles.FLEX,
          Styles.FLEX_ROW,
          Styles.LEFT_1_2,
          Styles.MAX_W_MD,
          Styles.OPACITY_0,
          Styles.PX_2,
          Styles.PY_2,
          Styles.ROUNDED_SM,
          Styles.SHADOW_LG,
          Styles._TRANSLATE_X_1_2,
          Styles.TEXT_GRAY_700,,
          Styles.TOP_2
          Styles.TRANSITION_OPACITY,
          Styles.TRANSFORM);

  private String ALERT_CLASSES = StyleUtils.joinStyles(Styles.BG_GRAY_200, Styles.BORDER_GRAY_300);

  private String WARNING_CLASSES =
      StyleUtils.joinStyles(Styles.BG_YELLOW_200, Styles.BORDER_YELLOW_300);

  /** Default duration is 3 seconds. */
  private int duration = 3000;

  private boolean canDismiss = false;

  public static ToastMessage alert(String message) {
    return new ToastMessage().setType(ToastType.ALERT).setMessage(message);
  }

  public static ToastMessage warning(String message) {
    return new ToastMessage().setType(ToastType.WARNING).setMessage(message);
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
        // TODO: change alert svg.
        wrappedWarningSvg.with(
            Icons.svg(Icons.WARNING_SVG_PATH, 20)
                .attr("fill-rule", "evenodd")
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

    if (canDismiss) {
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
