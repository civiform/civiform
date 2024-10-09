package services.pagination;

import io.ebean.ExpressionList;
import java.time.Instant;
import java.util.Date;
import models.ApplicationModel;

public class SubmitTimePaginationSpec extends BasePaginationSpec {

  // Static object helpers.
  public static SubmitTimePaginationSpec APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC =
      new SubmitTimePaginationSpec(
          Integer.MAX_VALUE, Instant.MAX, Long.MAX_VALUE);

  private final Instant currentSubmitTime;
  private final Long currentRowId;

  public SubmitTimePaginationSpec(int pageSize, Instant currentSubmitTime, Long currentRowId) {
    super(pageSize);
    this.currentSubmitTime = currentSubmitTime;
    this.currentRowId = currentRowId;
  }

  private Instant getCurrentSubmitTime() {
    return this.currentSubmitTime;
  }

  private Long getCurrentRowId() {
    return this.currentRowId;
  }

  protected <T> ExpressionList<T> applyOrderBy(ExpressionList<T> query) {
    return query.orderBy("submitTime desc, id desc");
  }

  protected <T> ExpressionList<T> maybeApplyWhere(ExpressionList<T> query) {
    // Date.from(Instant.MAX) is not supported, (overflows). If that is current
    // submit time in the spec, then skip setting a submitTime in the where
    // clause(), since all values in the database should be before the
    // Instant.MAX Date.
    if (this.getCurrentSubmitTime() == Instant.MAX) {
      return query.where().lt("id", this.getCurrentRowId());
    }
    return query
        .where()
        .or()
        .and()
        .eq("submitTime", Date.from(this.getCurrentSubmitTime()))
        .lt("id", this.getCurrentRowId())
        .lt("submitTime", this.getCurrentRowId());
  }
}
