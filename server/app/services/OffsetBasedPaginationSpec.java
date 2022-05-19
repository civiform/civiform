package services;

import java.util.Optional;

/**
 * Specifies pagination behavior for a query with offset identifier-based pagination. An offset
 * identifier identifies the last item of the previous page of results using its sort order
 * attribute.
 */
public class OffsetBasedPaginationSpec<T> extends PaginationSpec {

  public static OffsetBasedPaginationSpec<Long> MAX_PAGE_SIZE_SPEC_LONG =
      new OffsetBasedPaginationSpec<>(Integer.MAX_VALUE);

  private Optional<T> currentPageOffsetIdentifier;

  public OffsetBasedPaginationSpec(int pageSize) {
    super(pageSize);
    this.currentPageOffsetIdentifier = Optional.empty();
  }

  public OffsetBasedPaginationSpec(int pageSize, Optional<T> currentPageOffsetIdentifier) {
    super(pageSize);
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
