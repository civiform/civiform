package services;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;

public class PaginationResult<T> {
  private final PaginationSpec spec;
  private final int numPages;
  private final ImmutableList<T> pageContents;

  public PaginationResult(PaginationSpec spec, int numPages, ImmutableList<T> pageContents) {
    this.spec = checkNotNull(spec);
    this.numPages = numPages;
    this.pageContents = checkNotNull(pageContents);
  }

  public int getPageSize() {
    return this.spec.getPageSize();
  }

  public int getCurrentPage() {
    return this.spec.getCurrentPage();
  }

  public int getNumPages() {
    return this.numPages;
  }

  public ImmutableList<T> getPageContents() {
    return this.pageContents;
  }
}
