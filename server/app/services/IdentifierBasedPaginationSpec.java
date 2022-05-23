package services;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

/**
 * Specifies pagination behavior for a query using identifier-based offset pagination. An offset
 * identifier identifies the last item of the previous page of results using its sort order
 * attribute. The type of the identifier {@code T} depends on the type of the underlying attribute
 * used for sort order. This type of pagination is typically used in the API.
 */
public class IdentifierBasedPaginationSpec<T> {

  public static IdentifierBasedPaginationSpec<Long> MAX_PAGE_SIZE_SPEC_LONG =
      new IdentifierBasedPaginationSpec<>(Integer.MAX_VALUE);

  private final int pageSize;
  private final Optional<T> currentPageOffsetIdentifier;

  public IdentifierBasedPaginationSpec(int pageSize) {
    this.pageSize = pageSize;
    this.currentPageOffsetIdentifier = Optional.empty();
  }

  public IdentifierBasedPaginationSpec(int pageSize, Optional<T> currentPageOffsetIdentifier) {
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
