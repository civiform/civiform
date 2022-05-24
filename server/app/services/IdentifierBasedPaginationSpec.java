package services;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Specifies pagination behavior for a query using identifier-based offset pagination. An offset
 * identifier identifies the last item of the previous page of results using its sort order
 * attribute. The type of the identifier {@code T} depends on the type of the underlying attribute
 * used for sort order. This type of pagination is typically used in the API.
 */
public class IdentifierBasedPaginationSpec<T> {

  public static IdentifierBasedPaginationSpec<Long> MAX_PAGE_SIZE_SPEC_LONG =
      new IdentifierBasedPaginationSpec<>(Integer.MAX_VALUE, Long.MAX_VALUE);

  private final int pageSize;
  private final T currentPageOffsetIdentifier;

  public IdentifierBasedPaginationSpec(int pageSize, T currentPageOffsetIdentifier) {
    this.pageSize = pageSize;
    this.currentPageOffsetIdentifier = checkNotNull(currentPageOffsetIdentifier);
  }

  public int getPageSize() {
    return this.pageSize;
  }

  /**
   * Get the offset identifier for this page of results.
   *
   * <p>The identifier is a value that can be
   * compared to the sort order attribute of a paginated resource to offset a query. For example, if
   * a query is sorted by database ID, the offset identifier will be the database ID of the last
   * entry in the last page of results.
   */
  public T getCurrentPageOffsetIdentifier() {
    return this.currentPageOffsetIdentifier;
  }
}
