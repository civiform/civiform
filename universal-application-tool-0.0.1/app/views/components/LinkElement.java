package views.components;

import static j2html.TagCreator.a;

import com.google.common.base.Strings;
import j2html.tags.Tag;
import views.BaseStyles;
import views.StyleUtils;
import views.Styles;

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

  public Tag asAnchorText() {
    Tag ret = a(text).withHref(href).withClasses(DEFAULT_LINK_STYLES, styles);
    if (!this.id.isEmpty()) {
      ret.withId(id);
    }
    return ret;
  }

  public Tag asButton() {
    return a(text).withCondId(!Strings.isNullOrEmpty(id),id).withHref(href).withClasses(DEFAULT_LINK_BUTTON_STYLES, styles);    
  }
}
