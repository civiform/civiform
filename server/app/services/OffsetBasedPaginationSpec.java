package services;

import java.util.Optional;

/** Specifies pagination behavior for a query with offset-identifier-based pagination. */
public class OffsetBasedPaginationSpec<T> extends PaginationSpec {

  public static OffsetBasedPaginationSpec<Long> MAX_PAGE_SIZE_SPEC_LONG =
      new OffsetBasedPaginationSpec<>(Integer.MAX_VALUE);

  private Optional<T> currentPageOffsetIdentifier;

  public OffsetBasedPaginationSpec(int pageSize) {
    super(pageSize, 1);
    this.currentPageOffsetIdentifier = Optional.empty();
  }

  public OffsetBasedPaginationSpec(
      int pageSize, int currentPage, Optional<T> currentPageOffsetIdentifier) {
    super(pageSize, currentPage);
    this.currentPageOffsetIdentifier = currentPageOffsetIdentifier;
  }

  public OffsetBasedPaginationSpec<T> setCurrentPageOffsetIdentifier(T pageOffsetIdentifier) {
    this.currentPageOffsetIdentifier = Optional.of(pageOffsetIdentifier);
    return this;
  }

  public Optional<T> getCurrentPageOffsetIdentifier() {
    return this.currentPageOffsetIdentifier;
  }
}
