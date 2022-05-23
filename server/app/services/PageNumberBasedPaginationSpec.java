package services;


/**
 * Specifies pagination behavior for a query with page number-based pagination. With page-number
 * based pagination, the current page of results is resolved by multiplying the page size by the
 * current page - 1 to derive an offset for the underlying query. This type of pagination is
 * typically used in the UI. Page numbers are 1-indexed.
 */
public class PageNumberBasedPaginationSpec {

  public static final PageNumberBasedPaginationSpec MAX_PAGE_SIZE_SPEC =
      new PageNumberBasedPaginationSpec(Integer.MAX_VALUE, 1);

  private final int pageSize;
  private final Integer currentPage;

  public PageNumberBasedPaginationSpec(int pageSize) {
    this(pageSize, 1);
  }

  public PageNumberBasedPaginationSpec(int pageSize, int currentPage) {
    this.pageSize = pageSize;
    this.currentPage = currentPage;
  }

  public int getPageSize() {
    return this.pageSize;
  }

  /**
   * The current page number. Note that page numbers are 1-indexed (the first page has a page number
   * of 1).
   */
  public int getCurrentPage() {
    return this.currentPage;
  }

  public int getCurrentPageOffset() {
    return (currentPage - 1) * pageSize;
  }
}
