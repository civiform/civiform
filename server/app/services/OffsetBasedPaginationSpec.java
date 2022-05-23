package services;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

/**
 * Specifies pagination behavior for a query with offset identifier-based pagination. An offset
 * identifier identifies the last item of the previous page of results using its sort order
 * attribute.
 */
public class OffsetBasedPaginationSpec<T> {

  public static OffsetBasedPaginationSpec<Long> MAX_PAGE_SIZE_SPEC_LONG =
      new OffsetBasedPaginationSpec<>(Integer.MAX_VALUE);

  private final int pageSize;
  private final Optional<T> currentPageOffsetIdentifier;

  public OffsetBasedPaginationSpec(int pageSize) {
    this.pageSize = pageSize;
    this.currentPageOffsetIdentifier = Optional.empty();
  }

  public OffsetBasedPaginationSpec(int pageSize, Optional<T> currentPageOffsetIdentifier) {
    this.pageSize = pageSize;
    this.currentPageOffsetIdentifier = checkNotNull(currentPageOffsetIdentifier);
  }

  public int getPageSize() {
    return this.pageSize;
  }

  public Optional<T> getCurrentPageOffsetIdentifier() {
    return this.currentPageOffsetIdentifier;
  }
}
