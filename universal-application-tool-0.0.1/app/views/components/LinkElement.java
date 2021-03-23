package views.components;

import static j2html.TagCreator.a;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import play.filters.csrf.CSRF;
import play.mvc.Http;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

public class LinkElement {

  private static final String DEFAULT_LINK_BUTTON_STYLES =
      StyleUtils.joinStyles(
          Styles.FLOAT_LEFT,
          Styles.INLINE_BLOCK,
          Styles.CURSOR_POINTER,
          Styles.P_2,
          Styles.M_2,
          Styles.ROUNDED_MD,
          Styles.RING_BLUE_200,
          Styles.RING_OFFSET_2,
          Styles.BG_BLUE_400,
          Styles.TEXT_WHITE,
          StyleUtils.hover(Styles.BG_BLUE_500),
          StyleUtils.focus(Styles.OUTLINE_NONE, Styles.RING_2));

  private static final String DEFAULT_LINK_STYLES =
      StyleUtils.joinStyles(BaseStyles.LINK_TEXT, BaseStyles.LINK_HOVER_TEXT);

  private String id = "";
  private String text = "";
  private String href = "";
  private String styles = "";

  public LinkElement setId(String id) {
    this.id = id;
    return this;
  }

  public LinkElement setHref(String href) {
    this.href = href;
    return this;
  }

  public LinkElement setText(String text) {
    this.text = text;
    return this;
  }

  public LinkElement setStyles(String... styles) {
    this.styles = StyleUtils.joinStyles(styles);
    return this;
  }

  public ContainerTag asAnchorText() {
    ContainerTag tag = Strings.isNullOrEmpty(href) ? div(text) : a(text).withHref(href);
    return tag.withCondId(!Strings.isNullOrEmpty(id), id)
        .withCondHref(!Strings.isNullOrEmpty(href), href)
        .withClasses(DEFAULT_LINK_STYLES, styles);
  }

  public ContainerTag asButton() {
    ContainerTag tag = Strings.isNullOrEmpty(href) ? div(text) : a(text).withHref(href);
    return tag.withCondId(!Strings.isNullOrEmpty(id), id)
        .withClasses(DEFAULT_LINK_BUTTON_STYLES, styles);
  }

  public ContainerTag asHiddenForm(Http.Request request) {
    Preconditions.checkNotNull(href);
    return form(
            input()
                .isHidden()
                .withValue(CSRF.getToken(request.asScala()).get().value())
                .withName("csrfToken"),
            button(TagCreator.text(text)).withType("submit"))
        .withMethod("POST")
        .withAction(href)
        .withCondId(!Strings.isNullOrEmpty(id), id);
  }
}
