package mapping.admin.shared;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import views.admin.shared.LegacyPagination;
import views.admin.shared.LegacyPagination.Item;

/**
 * Builds a {@link LegacyPagination} using the same slot rules as the legacy j2html {@code
 * BaseHtmlView.renderPagination}: at most seven slots, the first and last page always shown, and
 * ellipses standing in for the pages that don't fit.
 */
public final class LegacyPaginationMapper {

  /**
   * Maps the pagination state to the view model.
   *
   * @param page the current (1-based) page number
   * @param pageCount the total number of pages
   * @param urlForPage renders the link for a given page number
   */
  public LegacyPagination map(int page, int pageCount, IntFunction<String> urlForPage) {
    Preconditions.checkArgument(pageCount >= 1, "pageCount must be at least 1");
    Preconditions.checkArgument(page >= 1, "page must be at least 1");

    boolean isCurrentPageInMiddle = page > 4 && page < (pageCount - 3);
    boolean isCurrentPageInLastFour = page >= (pageCount - 3);

    ImmutableList.Builder<Item> items = ImmutableList.builder();
    items.add(pageItem(1, page, urlForPage));

    if (pageCount <= 7) {
      // No ellipses: every page gets a button.
      IntStream.rangeClosed(2, pageCount)
          .forEach(pageNum -> items.add(pageItem(pageNum, page, urlForPage)));
    } else if (isCurrentPageInMiddle) {
      // [1] [...] [page-1] [page] [page+1] [...] [last]
      items.add(Item.ellipsis());
      items.add(pageItem(page - 1, page, urlForPage));
      items.add(pageItem(page, page, urlForPage));
      items.add(pageItem(page + 1, page, urlForPage));
      items.add(Item.ellipsis());
      items.add(pageItem(pageCount, page, urlForPage));
    } else if (page <= 4) {
      // [1] [2] [3] [4] [5] [...] [last]
      IntStream.rangeClosed(2, 5)
          .forEach(pageNum -> items.add(pageItem(pageNum, page, urlForPage)));
      items.add(Item.ellipsis());
      items.add(pageItem(pageCount, page, urlForPage));
    } else if (isCurrentPageInLastFour) {
      // [1] [...] [last-4] [last-3] [last-2] [last-1] [last]
      items.add(Item.ellipsis());
      IntStream.rangeClosed(pageCount - 4, pageCount)
          .forEach(pageNum -> items.add(pageItem(pageNum, page, urlForPage)));
    }

    return LegacyPagination.builder()
        .previousPageUrl(page != 1 ? urlForPage.apply(page - 1) : null)
        .nextPageUrl(page != pageCount ? urlForPage.apply(page + 1) : null)
        .items(items.build())
        .build();
  }

  private static Item pageItem(int pageNumber, int currentPage, IntFunction<String> urlForPage) {
    return Item.page(pageNumber, urlForPage.apply(pageNumber), pageNumber == currentPage);
  }
}
