package views.components;

import static j2html.TagCreator.a;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.attributes.IHref;
import j2html.tags.attributes.ITarget;
import play.filters.csrf.CSRF;
import play.mvc.Http;
import scala.Option;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

/** Utility class for rendering link elements. */
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
          Styles.BORDER,
          Styles.BORDER_TRANSPARENT,
          BaseStyles.BG_SEATTLE_BLUE,
          Styles.TEXT_WHITE,
          StyleUtils.hover(Styles.BG_BLUE_700),
          StyleUtils.focus(Styles.OUTLINE_NONE, Styles.RING_2));
  private static final String RIGHT_ALIGNED_LINK_BUTTON_STYLES =
      StyleUtils.joinStyles(
          Styles.FLOAT_RIGHT,
          Styles._MT_14,
          Styles.INLINE_BLOCK,
          Styles.CURSOR_POINTER,
          Styles.P_2,
          Styles.M_2,
          Styles.ROUNDED_MD,
          Styles.RING_BLUE_200,
          Styles.RING_OFFSET_2,
          Styles.BORDER,
          Styles.BORDER_TRANSPARENT,
          BaseStyles.BG_SEATTLE_BLUE,
          Styles.TEXT_WHITE,
          StyleUtils.hover(Styles.BG_BLUE_700),
          StyleUtils.focus(Styles.OUTLINE_NONE, Styles.RING_2));

  private static final String DEFAULT_LINK_STYLES =
      StyleUtils.joinStyles(BaseStyles.LINK_TEXT, BaseStyles.LINK_HOVER_TEXT);

  private static final String BUTTON_LOOKS_LIKE_LINK_STYLES =
      StyleUtils.joinStyles(
          Styles.BORDER_NONE,
          Styles.CURSOR_POINTER,
          Styles.BG_TRANSPARENT,
          StyleUtils.hover(Styles.BG_TRANSPARENT),
          DEFAULT_LINK_STYLES,
          Styles.P_0,
          Styles.MR_2,
          Styles.FONT_NORMAL);

  private String id = "";
  private String text = "";
  private String href = "";
  private String styles = "";
  private String onsubmit = "";
  private boolean doesOpenInNewTab = false;

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

  public LinkElement setOnsubmit(String onsubmit) {
    this.onsubmit = onsubmit;
    return this;
  }

  public LinkElement opensInNewTab() {
    this.doesOpenInNewTab = true;
    return this;
  }

  public ATag asAnchorText() {
    ATag tag = a(text);
    return tag.withCondId(!Strings.isNullOrEmpty(id), id)
        .withCondHref(!Strings.isNullOrEmpty(href), href)
        .withCondTarget(doesOpenInNewTab, "_blank")
        .withClasses(DEFAULT_LINK_STYLES, styles);
  }

  private <T extends ContainerTag<T> & IHref<T> & ITarget<T>> void setTargetMaybeHref(T tag) throws RuntimeException {
    if (tag.getTagName().equals("a") && Strings.isNullOrEmpty(href)) {
      throw new RuntimeException("trying to create an <a> tag with no href defined!");
    }
    tag.withCondHref(!Strings.isNullOrEmpty(href), href)
        .withCondTarget(doesOpenInNewTab, "_blank");
  }

  private void maybeSetId(Tag tag) {
    tag.withCondId(!Strings.isNullOrEmpty(id), id);
  }

  public ATag asButton() {
    ATag tag = a(text);
    setTargetMaybeHref(tag);
    maybeSetId(tag);
    return tag.withClasses(DEFAULT_LINK_BUTTON_STYLES, styles);
  }

  public ATag asRightAlignedButton() {
    ATag tag = a(text);
    setTargetMaybeHref(tag);
    maybeSetId(tag);
    return tag.withClasses(RIGHT_ALIGNED_LINK_BUTTON_STYLES, styles);
  }

  public DivTag asButtonNoHref() {
    DivTag tag = div(text);
    maybeSetId(tag);
    return tag.withClasses(DEFAULT_LINK_BUTTON_STYLES, styles);
  }

  public DivTag asRightAlignedButtonNoHref() {
    DivTag tag = div(text);
    maybeSetId(tag);
    return tag.withClasses(RIGHT_ALIGNED_LINK_BUTTON_STYLES, styles);
  }

  public FormTag asHiddenForm(Http.Request request) {
    return this.asHiddenForm(request, ImmutableMap.of());
  }

  public FormTag asHiddenForm(Http.Request request, ImmutableMap<String, String> hiddenFormValues) {
    Preconditions.checkNotNull(href);
    Option<CSRF.Token> csrfTokenMaybe = CSRF.getToken(request.asScala());
    String csrfToken = "";
    if (csrfTokenMaybe.isDefined()) {
      csrfToken = csrfTokenMaybe.get().value();
    }

    FormTag form =
        form(
                input().isHidden().withValue(csrfToken).withName("csrfToken"),
                button(TagCreator.text(text))
                    .withClasses(DEFAULT_LINK_BUTTON_STYLES)
                    .withType("submit"))
            .withMethod("POST")
            .withCondOnsubmit(!Strings.isNullOrEmpty(onsubmit), onsubmit)
            .withAction(href)
            .withCondId(!Strings.isNullOrEmpty(id), id);
    hiddenFormValues.entrySet().stream()
        .map(entry -> input().isHidden().withName(entry.getKey()).withValue(entry.getValue()))
        .forEach(tag -> form.with(tag));
    return form;
  }

  public FormTag asHiddenFormLink(Http.Request request) {
    Preconditions.checkNotNull(href);
    Option<CSRF.Token> csrfTokenMaybe = CSRF.getToken(request.asScala());
    String csrfToken = "";
    if (csrfTokenMaybe.isDefined()) {
      csrfToken = csrfTokenMaybe.get().value();
    }

    FormTag form =
        form(
                input().isHidden().withValue(csrfToken).withName("csrfToken"),
                button(TagCreator.text(text))
                    .withClasses(BUTTON_LOOKS_LIKE_LINK_STYLES)
                    .withType("submit"))
            .withClasses(Styles.INLINE)
            .withMethod("POST")
            .withCondOnsubmit(!Strings.isNullOrEmpty(onsubmit), onsubmit)
            .withAction(href)
            .withCondId(!Strings.isNullOrEmpty(id), id);
    return form;
  }
}
