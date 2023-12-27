package views.components.breadcrumb;

import static j2html.TagCreator.a;
import static j2html.TagCreator.each;
import static j2html.TagCreator.li;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.LiTag;
import j2html.tags.specialized.NavTag;
import j2html.tags.specialized.OlTag;
import javax.annotation.Nullable;
import views.components.Icons;

/**
 * Factory class for creating the breadcrumb UI component. See <a
 * href="https://designsystem.digital.gov/components/breadcrumb/">USWDS Breadcrumb
 * Documentation</a>.
 */
public final class BreadcrumbFactory {

  /**
   * Builds a breadcrumb UI component based off the given list.
   *
   * @param breadcrumbItems the list of items to include in the breadcrumb. Must be non-empty. The
   *     order of items in the list determines the breadcrumb order.
   * @throws IllegalArgumentException if {@code breadcrumbItems} is empty.
   * @throws IllegalArgumentException if multiple breadcrumb items have {@code
   *     BreadcrumbItem#isCurrentPage()} marked as true.
   */
  public NavTag buildBreadcrumb(ImmutableList<BreadcrumbItem> breadcrumbItems) {
    if (breadcrumbItems.isEmpty()) {
      throw new IllegalArgumentException("breadcrumbItems must be non-empty");
    }
    if (breadcrumbItems.stream()
            .map(BreadcrumbItem::isCurrentPage)
            .filter(isCurrent -> isCurrent)
            .count()
        > 1) {
      throw new IllegalArgumentException(
          "At most 1 breadcrumb item can be marked as being the current page");
    }

    NavTag breadcrumbNav =
        new NavTag().withClasses("usa-breadcrumb").attr("aria-label", "Breadcrumbs");
    // Padding is required for tabbing focus to render correctly.
    OlTag breadcrumbContainer = new OlTag().withClasses("usa-breadcrumb__list", "flex", "p-2");

    breadcrumbContainer.with(each(breadcrumbItems, this::createBreadcrumbHtml));
    return breadcrumbNav.with(breadcrumbContainer);
  }

  private LiTag createBreadcrumbHtml(BreadcrumbItem breadcrumbItem) {
    LiTag liTag = li().withClasses("usa-breadcrumb__list-item", "flex");
    if (breadcrumbItem.link() != null) {
      ATag atag = a().withClasses("usa-breadcrumb__link", "flex");
      addIconIfNeeded(breadcrumbItem.icon(), atag);
      atag.withHref(breadcrumbItem.link());
      atag.with(span(breadcrumbItem.text()));
      liTag.with(atag);
    } else {
      addIconIfNeeded(breadcrumbItem.icon(), liTag);
      liTag.with(span(breadcrumbItem.text()));
    }
    if (breadcrumbItem.isCurrentPage()) {
      liTag.attr("aria-current", "page");
    }
    return liTag;
  }

  private void addIconIfNeeded(@Nullable Icons icon, ContainerTag<?> container) {
    if (icon != null) {
      container.with(Icons.svg(icon).withClasses("flex", "inline-block", "h-5", "w-5", "mr-1"));
    }
  }
}
