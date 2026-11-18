package views.admin.shared;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;

/**
 * Presentation model for the legacy USWDS pagination control rendered by {@code
 * admin/shared/LegacyPagination.html}. Mirrors the DOM of {@code BaseHtmlView.renderPagination} for
 * admin pages migrated 1:1 from j2html.
 *
 * <p>Build instances with {@link mapping.admin.shared.LegacyPaginationMapper}, which decides which
 * page buttons and ellipses to show.
 */
@Data
@Builder
public final class LegacyPagination {

  // Link to the previous page, null on the first page (hides the arrow).
  private final String previousPageUrl;

  // Link to the next page, null on the last page (hides the arrow).
  private final String nextPageUrl;

  // Page buttons and ellipses between the arrows, in render order.
  private final ImmutableList<Item> items;

  /** One slot in the pagination list: either a page button or an ellipsis. */
  @Data
  @Builder
  public static final class Item {
    private final boolean ellipsis;

    // Page number and link; unset (0 / null) for an ellipsis.
    private final int pageNumber;
    private final String url;

    // Whether this page button is the current page.
    private final boolean current;

    public static Item ellipsis() {
      return Item.builder().ellipsis(true).build();
    }

    public static Item page(int pageNumber, String url, boolean current) {
      return Item.builder().pageNumber(pageNumber).url(url).current(current).build();
    }
  }
}
