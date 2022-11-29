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
import j2html.tags.specialized.TableTag;
import j2html.tags.specialized.TrTag;
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
    TableTag content =
        table()
            .with(
                tr().with(th("Icon name"), th("Icon"), th("Width"), th("Height")),
                each(ImmutableList.copyOf(Icons.values()), this::renderIconRow));
    HtmlBundle bundle =
        layout
            .getBundle()
            .setTitle("Icons")
            .addMainContent(content)
            .addFooterScripts(layout.viewUtils.makeLocalJsTag("dev_icons"));
    return layout.render(bundle);
  }

  private TrTag renderIconRow(Icons icon) {
    return tr().with(
            td(icon.name()),
            td(Icons.svg(icon).withClasses("h-6", "w-6")),
            td(p("").withClass("icon-width")),
            td(p("").withClass("icon-height")));
  }
}
