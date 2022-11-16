package views.components;

import static com.google.common.base.Preconditions.checkNotNull;
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
import j2html.tags.attributes.IHref;
import j2html.tags.attributes.ITarget;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import java.util.Optional;
import play.filters.csrf.CSRF;
import play.mvc.Http;
import scala.Option;
import views.style.BaseStyles;
import views.style.StyleUtils;

/** Utility class for rendering link elements. */
public final class LinkElement {

  private static final String DEFAULT_LINK_BUTTON_STYLES =
      StyleUtils.joinStyles(
          "float-left",
          "inline-block",
          "cursor-pointer",
          "p-2",
          "m-2",
          "rounded-md",
          "ring-blue-200",
          "ring-offset-2",
          "border",
          "border-transparent",
          BaseStyles.BG_SEATTLE_BLUE,
          "text-white",
          StyleUtils.hover("bg-blue-700"),
          StyleUtils.focus("outline-none", "ring-2"));
  private static final String RIGHT_ALIGNED_LINK_BUTTON_STYLES =
      StyleUtils.joinStyles(
          "float-right",
          "inline-block",
          "cursor-pointer",
          "p-2",
          "m-2",
          "rounded-md",
          "ring-blue-200",
          "ring-offset-2",
          "border",
          "border-transparent",
          BaseStyles.BG_SEATTLE_BLUE,
          "text-white",
          StyleUtils.hover("bg-blue-700"),
          StyleUtils.focus("outline-none", "ring-2"));

  private static final String DEFAULT_LINK_STYLES =
      StyleUtils.joinStyles(
          BaseStyles.LINK_TEXT, BaseStyles.LINK_HOVER_TEXT, "inline-flex", "items-center");

  private static final String BUTTON_LOOKS_LIKE_LINK_STYLES =
      StyleUtils.joinStyles(
          "border-none",
          "cursor-pointer",
          "bg-transparent",
          StyleUtils.hover("bg-transparent"),
          DEFAULT_LINK_STYLES,
          "p-0",
          "mr-2",
          "font-normal");

  private String id = "";
  private String text = "";
  private String href = "";
  private String styles = "";
  private String onsubmit = "";
  private boolean doesOpenInNewTab = false;
  private Optional<Icons> icon = Optional.empty();

  public enum IconPosition {
    NONE,
    START,
    END
  }

  private IconPosition iconPosition = IconPosition.START;

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

  public LinkElement setIcon(Icons icon) {
    this.icon = Optional.of(checkNotNull(icon));
    return this;
  }

  public LinkElement setIconPosition(IconPosition position) {
    this.iconPosition = position;
    return this;
  }

  public ATag asAnchorText() {
    ATag tag = a();
    if (this.iconPosition == IconPosition.END) {
      tag.withText(this.text);
      this.icon.ifPresent(icon -> tag.with(Icons.svg(icon).withClasses("mr-2", "w-5", "h-5")));
    } else {
      this.icon.ifPresent(icon -> tag.with(Icons.svg(icon).withClasses("mr-2", "w-5", "h-5")));
      tag.withText(this.text);
    }
    return tag.withCondId(!Strings.isNullOrEmpty(id), id)
        .withCondHref(!Strings.isNullOrEmpty(href), href)
        .withCondTarget(doesOpenInNewTab, "_blank")
        .withClasses(DEFAULT_LINK_STYLES, styles);
  }

  private <T extends ContainerTag<T> & IHref<T> & ITarget<T>> void setTargetMaybeHref(T tag) {
    if (tag.getTagName().equals("a") && Strings.isNullOrEmpty(href)) {
      throw new RuntimeException("trying to create an <a> tag with no href defined!");
    }
    tag.withCondHref(!Strings.isNullOrEmpty(href), href).withCondTarget(doesOpenInNewTab, "_blank");
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
            .withClasses("inline")
            .withMethod("POST")
            .withCondOnsubmit(!Strings.isNullOrEmpty(onsubmit), onsubmit)
            .withAction(href)
            .withCondId(!Strings.isNullOrEmpty(id), id);
    return form;
  }
}
