package views;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.li;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.LiTag;
import j2html.tags.specialized.NavTag;
import j2html.tags.specialized.OlTag;
import java.util.Optional;
import views.components.Icons;

public final class Breadcrumb {

  public NavTag createBreadcrumb(ImmutableList<LiTag> breadcrumbs) {
    NavTag breadcrumbNav =
        new NavTag()
            .withClass("usa-breadcrumb")
            .attr("aria-label", "Breadcrumbs"); // TODO: aria-label
    OlTag breadcrumbItems = new OlTag().withClass("usa-breadcrumb__list");

    breadcrumbItems.with(breadcrumbs);
    return breadcrumbNav.with(breadcrumbItems);
  }

  public LiTag createBreadcrumbItem(String text, Optional<String> href, Optional<Icons> icon) {
    LiTag liTag = li().withClass("usa-breadcrumb__list-item");
    if (href.isPresent()) {
      ATag atag = a().withClass("usa-breadcrumb__link");
      href.ifPresent(atag::withHref);
      // TODO: Doesn't work
      icon.ifPresent(
          icons ->
              atag.with(
                  div()
                      .withClasses("flex", "max-w-fit")
                      .with(Icons.svg(icons).withClasses("flex", "h-4.5", "w-4.5"))));
      atag.with(span(text));
      return liTag.with(atag);
    } else {
      return liTag.with(span(text)); // TODO: Image
    }
  }
}
