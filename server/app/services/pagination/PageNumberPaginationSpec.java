package services.pagination;

import io.ebean.Query;

/**
 * PageNumberPaginationSpec implements random access pagination into a list of rows sorted by either
 * id or SubmitTime.
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
 * <p>If orderBy expression is (submitTime, id) then an index over (submitTime, id) should exist for
 * the table being paged.
 */
public class PageNumberPaginationSpec extends BasePaginationSpec {

  public enum OrderByEnum {
    ID,
    SUBMIT_TIME,
  };

  // Static object helper definitions.
  public static PageNumberPaginationSpec MAX_PAGE_SIZE_BY_ID_SPEC =
      new PageNumberPaginationSpec(
          Integer.MAX_VALUE, /* currentPage= */ 1, PageNumberPaginationSpec.OrderByEnum.ID);
  public static PageNumberPaginationSpec MAX_PAGE_SIZE_BY_SUBMIT_TIME_SPEC =
      new PageNumberPaginationSpec(
          Integer.MAX_VALUE,
          /* currentPage= */ 1,
          PageNumberPaginationSpec.OrderByEnum.SUBMIT_TIME);

  private final int currentPage;
  private String orderBy;

  public PageNumberPaginationSpec(int pageSize, OrderByEnum orderBy) {
    super(pageSize);
    this.currentPage = 1;
    this.setOrderBy(orderBy);
  }

  public PageNumberPaginationSpec(int pageSize, int currentPage, OrderByEnum orderBy) {
    super(pageSize);
    this.currentPage = currentPage;
    this.setOrderBy(orderBy);
  }

  /**
   * The current page number. Note that page numbers are 1-indexed (the first page has a page number
   * of 1).
   */
  public int getCurrentPage() {
    return this.currentPage;
  }

  private void setOrderBy(OrderByEnum orderBy) {
    switch (orderBy) {
      case ID -> this.orderBy = "id desc";
      case SUBMIT_TIME -> this.orderBy = "submitTime desc, id desc";
    }
  }

  private int getCurrentPageOffset() {
    return (this.getCurrentPage() - 1) * this.getPageSize();
  }

  @Override
  protected <T> Query<T> applyOrderBy(Query<T> query) {
    return query.orderBy(this.orderBy);
  }

  @Override
  protected <T> Query<T> maybeApplySetFirstRow(Query<T> query) {
    return query.setFirstRow(this.getCurrentPageOffset());
  }
}
