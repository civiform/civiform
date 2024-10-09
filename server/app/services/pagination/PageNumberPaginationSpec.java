package services.pagination;

import io.ebean.ExpressionList;

/**
 * PageNumberPaginationSpec implements random access pagination into a list of rows sorted by
 * submitTime.
 *
 * <p>The table being paged must have the following columns defined: submitTime, id.
 *
 * <p>** If you wish to sort by a different column, create a new paging spec.
 *
 * <p>This spec is recommended for paging in a user interface view, where the user may choose to
 * skip ahead a few pages. Because the query must read all the rows up to the page being served, the
 * query performance wll degrade the higher the page number accessed. It is possible that rows may
 * be served more than once, or not at all if the underlying table query results change (due to a
 * row being added or removed).
 *
 * <p>No measurements of how fast performnace degrades have been done, but a guess would be that if
 * we have to read 100k objects before you get to the page you want, you might see a difference. To
 * mitigate this performance problem, it is recommended that the UI limit the number of pages the
 * user can see, and force users to filter their query to reduce the max number of rows in the
 * result to a reasonable number. Ie: 1,000? or 10,000.
 *
 * <p>The orderBy expression is (submitTime, id) which means that the order will stay stable for
 * multiple applications that have the same submit time. To avoid a performance penaly on these
 * queries an index over (submitTime, id) should exist.
 */
public class PageNumberPaginationSpec extends BasePaginationSpec {

  // Static object helper definitions.
  public static PageNumberPaginationSpec APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC =
      new PageNumberPaginationSpec(Integer.MAX_VALUE, 1);

  private final int currentPage;

  public PageNumberPaginationSpec(int pageSize) {
    super(pageSize);
    this.currentPage = 1;
  }

  public PageNumberPaginationSpec(int pageSize, int currentPage) {
    super(pageSize);
    this.currentPage = currentPage;
  }

  /**
   * The current page number. Note that page numbers are 1-indexed (the first page has a page number
   * of 1).
   */
  public int getCurrentPage() {
    return this.currentPage;
  }

  private int getCurrentPageOffset() {
    return (this.getCurrentPage() - 1) * this.getPageSize();
  }

  protected <T> ExpressionList<T> applyOrderBy(ExpressionList<T> query) {
    return query.orderBy("submitTime desc, id desc");
  }

  protected <T> ExpressionList<T> maybeApplySetFirstRow(ExpressionList<T> query) {
    return query.setFirstRow(this.getCurrentPageOffset());
  }
}
