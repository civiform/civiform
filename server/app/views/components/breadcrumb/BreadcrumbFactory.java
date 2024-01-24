package views.components.breadcrumb;

import static j2html.TagCreator.a;
import static j2html.TagCreator.li;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.LiTag;
import j2html.tags.specialized.NavTag;
import j2html.tags.specialized.OlTag;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import views.components.Icons;

/**
 * Factory class for creating the breadcrumb UI component. See <a
 * href="https://designsystem.digital.gov/components/breadcrumb/">USWDS Breadcrumb
 * Documentation</a>.
 *
 * <p>Note: Breadcrumbs aren't currently used because we want all admin pages to have consistent
 * navigation, so we should move all admin pages to breadcrumbs at once. See #6275.
 */
public final class BreadcrumbFactory {

  /**
   * Builds a breadcrumb UI component based off the given list.
   *
   * @param breadcrumbItems the list of items to include in the breadcrumb. Must be non-empty. The
   *     order of items in the list determines the breadcrumb order.
   * @throws IllegalArgumentException if {@code breadcrumbItems} is empty.
   */
  public NavTag buildBreadcrumbTrail(ImmutableList<BreadcrumbItem> breadcrumbItems) {
    if (breadcrumbItems.isEmpty()) {
      throw new IllegalArgumentException("breadcrumbItems must be non-empty");
    }

    NavTag breadcrumbNav =
        new NavTag().withClasses("usa-breadcrumb").attr("aria-label", "Breadcrumbs");
    // Padding is required for tabbing focus to render correctly.
    OlTag breadcrumbContainer = new OlTag().withClasses("usa-breadcrumb__list", "flex", "p-2");

    List<LiTag> breadcrumbItemsHtml =
        breadcrumbItems.stream().map(this::buildBreadcrumbTrailItem).collect(Collectors.toList());
    // We assume the last breadcrumb item represents the current page.
    breadcrumbItemsHtml.get(breadcrumbItemsHtml.size() - 1).attr("aria-current", "page");

    breadcrumbContainer.with(breadcrumbItemsHtml);
    return breadcrumbNav.with(breadcrumbContainer);
  }

  private LiTag buildBreadcrumbTrailItem(BreadcrumbItem breadcrumbItem) {
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
    return liTag;
  }

  private void addIconIfNeeded(@Nullable Icons icon, ContainerTag<?> container) {
    if (icon != null) {
      container.with(Icons.svg(icon).withClasses("flex", "inline-block", "h-5", "w-5", "mr-1"));
    }
  }
}
