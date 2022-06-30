package views.dev;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.each;
import static j2html.TagCreator.p;
import static j2html.TagCreator.table;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.tr;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.Icons;

/**
 * Renders a listing of all SVG icons and metadata about the icon size. This is generally useful for
 * seeing what icons are already available and debugging any SVG scaling / sizing issues.
 */
public final class IconsView extends BaseHtmlView {
  private final BaseHtmlLayout layout;

  @Inject
  public IconsView(BaseHtmlLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render() {
    ContainerTag content =
        table()
            .with(
                tr().with(th("Icon name"), th("Icon"), th("Width"), th("Height")),
                each(ImmutableList.copyOf(Icons.values()), icon -> renderIconRow(icon)));
    HtmlBundle bundle =
        layout
            .getBundle()
            .setTitle("Icons")
            .addMainContent(content)
            .addFooterScripts(layout.viewUtils.makeLocalJsTag("dev_icons"));
    return layout.render(bundle);
  }

  private Tag renderIconRow(Icons icon) {
    return tr().with(
            td(icon.name()),
            td(Icons.svg(icon.path, 24)),
            td(p("").withClass("icon-width")),
            td(p("").withClass("icon-height")));
  }
}
