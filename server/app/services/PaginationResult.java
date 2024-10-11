package services;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;

// TODO(#8841): Move this class into the pagination subpackage.

/** Contains a single page of results and pagination state for a paginated query. */
public class PaginationResult<T> {
  private final boolean hasNext;
  private final int numPages;
  private final ImmutableList<T> pageContents;

  public PaginationResult(boolean hasNext, int numPages, ImmutableList<T> pageContents) {
    this.hasNext = hasNext;
    this.numPages = numPages;
    this.pageContents = checkNotNull(pageContents);
  }

  public boolean hasMorePages() {
    return hasNext;
  }

  public int getNumPages() {
    return this.numPages;
  }

  public ImmutableList<T> getPageContents() {
    return this.pageContents;
  }
}
