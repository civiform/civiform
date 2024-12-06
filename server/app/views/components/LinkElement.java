package views.components;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;

import com.google.common.base.Strings;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import j2html.tags.attributes.IHref;
import j2html.tags.attributes.ITarget;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
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
          BaseStyles.BG_CIVIFORM_BLUE,
          "text-white",
          StyleUtils.hover("bg-blue-700"),
          StyleUtils.focus("outline-none", "ring-2"));

  private static final String DEFAULT_LINK_STYLES =
      StyleUtils.joinStyles(
          BaseStyles.LINK_TEXT, BaseStyles.LINK_HOVER_TEXT, "inline-flex", "items-center");

  private String id = "";
  private String text = "";
  private String href = "";
  private String styles = "";
  private boolean doesOpenInNewTab = false;
  private Optional<Icons> icon = Optional.empty();

  public enum IconPosition {
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

  public LinkElement opensInNewTab() {
    this.doesOpenInNewTab = true;
    return this;
  }

  public LinkElement setIcon(Icons icon, IconPosition position) {
    this.icon = Optional.of(checkNotNull(icon));
    this.iconPosition = position;
    return this;
  }

  public ATag asAnchorText() {
    ATag tag = a();
    if (IconPosition.END.equals(this.iconPosition)) {
      tag.withText(this.text);
      this.icon.ifPresent(icon -> tag.with(Icons.svg(icon).withClasses("mr-2", "w-5", "h-5")));
    } else {
      this.icon.ifPresent(icon -> tag.with(Icons.svg(icon).withClasses("mr-2", "w-5", "h-5")));
      tag.withText(this.text);
    }
    return tag.withCondId(!Strings.isNullOrEmpty(id), id)
        .withCondHref(!Strings.isNullOrEmpty(href), href)
        .withCondTarget(doesOpenInNewTab, "_blank")
        .withCondData(doesOpenInNewTab, "rel", "noopener noreferrer")
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

  public DivTag asButtonNoHref() {
    DivTag tag = div(text);
    maybeSetId(tag);
    return tag.withClasses(DEFAULT_LINK_BUTTON_STYLES, styles);
  }
}
