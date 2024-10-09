package services.pagination;

import io.ebean.ExpressionList;
import java.time.Instant;
import java.util.Date;

/**
 * SubmitTimePaginationSpec implements sequential paging access into a list of rows sorted by
 * the submitTime column.
 * 
 * The table being paged must have the following columns defined: submitTime, id.
 * 
 * ** If you wish to sort by a different column, create a new paging spec.
 * 
 * This spec is recommended for paging in an CSV/JSON export method, where every page in the
 * result will be accessed in a sequential manner. Access performance to every page is
 * constant. No items will be missed by this paging spec.
 * 
 * The orderBy expression is (submitTime, id) which means that the order will stay stable
 * for multiple applications that have the same submit time. To avoid a performance penaly
 * on these queries an index over (submitTime, id) should exist.
 */
public class SubmitTimePaginationSpec extends BasePaginationSpec {

  // Static object helper definitions.
  public static SubmitTimePaginationSpec APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC =
      new SubmitTimePaginationSpec(Integer.MAX_VALUE, Instant.MAX, Long.MAX_VALUE);

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
