package services;

import java.util.Optional;

/**
 * Specifies pagination behavior for a query with page number-based pagination. With page-number
 * based pagination, the current page of results is resolved by multiplying the page size by the
 * current page - 1 to derive an offset for the the underlying query. This type of pagination is
 * typically used in the UI.
 */
public class PageNumberBasedPaginationSpec {

  public static PageNumberBasedPaginationSpec MAX_PAGE_SIZE_SPEC =
      new PageNumberBasedPaginationSpec(Integer.MAX_VALUE, 1);

  private final int pageSize;
  private final Optional<Integer> currentPage;

  public PageNumberBasedPaginationSpec(int pageSize) {
    this.pageSize = pageSize;
    this.currentPage = Optional.empty();
  }

  public PageNumberBasedPaginationSpec(int pageSize, int currentPage) {
    this.pageSize = pageSize;
    this.currentPage = Optional.of(currentPage);
  }

  public int getPageSize() {
    return this.pageSize;
  }

  public int getCurrentPage() {
    return this.currentPage.get();
  }
}
