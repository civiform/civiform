package services.pagination;

import io.ebean.ExpressionList;
import models.ApplicationModel;

public class RowIdPaginationSpec extends BasePaginationSpec {

  // Static object helpers.
  public static RowIdPaginationSpec APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC =
      new RowIdPaginationSpec(Integer.MAX_VALUE, Long.MAX_VALUE);

  private final Long currentRowId;

  public RowIdPaginationSpec(int pageSize, Long currentRowId) {
    super(pageSize);
    this.currentRowId = currentRowId;
  }

  private Long getCurrentRowId() {
    return this.currentRowId;
  }

  protected <T> ExpressionList<T> applyOrderBy(ExpressionList<T> query) {
    return query.orderBy("id desc");
  }

  protected <T> ExpressionList<T> maybeApplyWhere(ExpressionList<T> query) {
    return query.where().lt("id", this.getCurrentRowId());
  }
}
