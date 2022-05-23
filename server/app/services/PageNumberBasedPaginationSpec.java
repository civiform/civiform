package services;

import java.util.Optional;

/** Specifies pagination behavior for a paginated query. */
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
