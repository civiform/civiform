package services.pagination;

import io.ebean.ExpressionList;
import models.ApplicationModel;

public class PageNumberPaginationSpec<T> extends BasePaginationSpec<T> {

  // Static object helpers.
  public static PageNumberPaginationSpec<ApplicationModel> APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC =
      new PageNumberPaginationSpec<ApplicationModel>(Integer.MAX_VALUE, 1);

  private final int currentPage;

  public PageNumberPaginationSpec(int pageSize) {
    super(pageSize);
    this.currentPage = 1;
  }

  public PageNumberPaginationSpec(int pageSize, int currentPage) {
    super(pageSize);
    this.currentPage = currentPage;
  }

  public int getCurrentPage() {
    return this.currentPage;
  }

  private int getCurrentPageOffset() {
    return (this.getCurrentPage() - 1) * this.getPageSize();
  }

  protected ExpressionList<T> applyOrderBy(ExpressionList<T> query) {
    return query.orderBy("id desc");
  }

  protected ExpressionList<T> maybeApplySetFirstRow(ExpressionList<T> query) {
    return query.setFirstRow(this.getCurrentPageOffset());
  }
}
