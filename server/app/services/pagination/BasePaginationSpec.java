package services.pagination;

import io.ebean.Query;

/**
 * Abstracts different kind of pagination strategies.
 *
 * <p>PaginationSpecs come in 2 major flavors: random acess paging and sequential paging.
 *
 * <p>Random Access Paging: requires no knowledge of the last row that was displayed in a previous
 * page. This spec requires reading from storage all the rows prior to the page being returned,
 * performance degrades as higher pages are queried for. Usually used in an user-facing interface.
 *
 * <p>Sequential paging: requires knowledge of the last row returned by the previous page to
 * retrieve the next page. This spec has constant performance for any page in the range, but can
 * only access pages in sequence. Usually used for export functions and API retrieve methods.
 *
 * <p>Implementations of this interface are: PageNumberPaginationSpec, RowIdPaginationSpec, and
 * SubmitTimePaginationSpec.
 *
 * <p>PageNumberPaginationSpec: sorts by submit time, is a random access paging spec. Recommended
 * for user facing views.
 *
 * <p>RowIdPaginationSpec: sorts by row id, is a sequential access paging spec. Recommended for API
 * methods.
 *
 * <p>SubmitTimePaginationSpec: sorts by submit time, is a sequential access paging spec.
 * Recommended for CSV/JSON export methods.
 *
 * <p>Usage:
 *
 * <p>During query build-up for a list of Models, apply the pagination spec before calling
 * findPagedList.
 *
 * <p>Exmaple: ... PagedList<Foo> pagedQuery = paginationSpec.apply(query).findPagedList(); ...
 */
public abstract class BasePaginationSpec {
  private final int pageSize;

  public BasePaginationSpec(int pageSize) {
    this.pageSize = pageSize;
  }

  public int getPageSize() {
    return this.pageSize;
  }

  /**
   * Modifies the passed in query the following expressions: - setMaxRows: page size - orderBy:
   * desired sort order
   *
   * <p>one of the following expressions will also be added (at most one) - where: for sequential
   * page access - setFirstRow: for random page access
   *
   * @param <T> The model for query expression list
   * @param query a list of expressions for model
   * @return a list of expressions for model
   */
  public final <T> Query<T> apply(Query<T> query) {
    query = this.applySetMaxRows(query);
    query = this.applyOrderBy(query);
    query = this.maybeApplyWhere(query);
    query = this.maybeApplySetFirstRow(query);
    return query;
  }

  private final <T> Query<T> applySetMaxRows(Query<T> query) {
    return query.setMaxRows(this.pageSize);
  }

  /**
   * applyOrderBy must be implemented by the subclass, if the pagination being implemented is
   * sequential (pages are viewed in order), then the sort order columns should match the where
   * clause.
   */
  protected abstract <T> Query<T> applyOrderBy(Query<T> query);

  /** optionally implemented in the subclass if implementing a sequential access paging spec */
  protected <T> Query<T> maybeApplyWhere(Query<T> query) {
    return query;
  }

  /** optionally implemented in the subclass if implementing a random access paging spec. */
  protected <T> Query<T> maybeApplySetFirstRow(Query<T> query) {
    return query;
  }
}
