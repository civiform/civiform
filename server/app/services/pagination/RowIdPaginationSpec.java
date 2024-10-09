package services.pagination;

import io.ebean.Query;

/**
 * RowIdPaginationSpec implements sequential paging access into a list of rows sorted by the id
 * column.
 *
 * <p>The table being paged must have the following columns defined: id.
 *
 * <p>** If you wish to sort by a different column, create a new paging spec.
 *
 * <p>This spec is recommended for paging in an API retrieve method, where every page in the result
 * will be accessed in a sequential manner. Access performance to every page is constant. No items
 * will be missed by this paging spec.
 */
public class RowIdPaginationSpec extends BasePaginationSpec {

  // Static object helper definitions.
  public static RowIdPaginationSpec APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC =
      new RowIdPaginationSpec(Integer.MAX_VALUE, Long.MAX_VALUE);

  private final Long currentRowId;

  public RowIdPaginationSpec(int pageSize, Long currentRowId) {
    super(pageSize);
    this.currentRowId = currentRowId;
  }

  @Override
  protected <T> Query<T> applyOrderBy(Query<T> query) {
    return query.orderBy("id desc");
  }

  @Override
  protected <T> Query<T> maybeApplyWhere(Query<T> query) {
    return query.where().lt("id", this.currentRowId).query();
  }
}
