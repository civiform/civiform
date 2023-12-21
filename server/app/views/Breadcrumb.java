package views;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.LiTag;
import j2html.tags.specialized.NavTag;
import j2html.tags.specialized.OlTag;
import views.components.Icons;

import javax.swing.*;

import java.util.Optional;

import static j2html.TagCreator.a;
import static j2html.TagCreator.each;
import static j2html.TagCreator.li;
import static j2html.TagCreator.span;

public final class Breadcrumb {

    public NavTag createBreadcrumb(ImmutableList<LiTag> breadcrumbs) {
        NavTag breadcrumbNav = new NavTag().withClass("usa-breadcrumb").attr("aria-label", "Breadcrumbs"); // TODO: aria-label
        OlTag breadcrumbItems = new OlTag().withClass("use-breadcrumb__list");

        breadcrumbItems.with(breadcrumbs);
        return breadcrumbNav;
    }

    public LiTag createBreadcrumbItem(String text, String href, Optional<Icons> icon) {
        return li().withClass("use-breadcrumb__list-item")
                .with(a().withClass("usa-breadcrumb__link").withHref(href)
                        .condWith(icon.isPresent(), Icons.svg(icon.get()))
                        .with(
                        span(text)
                ));
    }
}
