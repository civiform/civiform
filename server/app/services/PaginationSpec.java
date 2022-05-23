package services;

import java.util.Optional;

/** Specifies pagination behavior for a paginated query. */
public class PaginationSpec {

  public static PaginationSpec MAX_PAGE_SIZE_SPEC = new PaginationSpec(Integer.MAX_VALUE, 1);

  private final int pageSize;
  private final Optional<Integer> currentPage;

  public PaginationSpec(int pageSize) {
    this.pageSize = pageSize;
    this.currentPage = Optional.empty();
  }

  public PaginationSpec(int pageSize, int currentPage) {
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
