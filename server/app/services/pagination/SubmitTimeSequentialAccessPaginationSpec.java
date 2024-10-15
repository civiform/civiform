package services.pagination;

import io.ebean.Query;
import java.time.Instant;

/**
 * SubmitTimeSequentialAccessPaginationSpec implements sequential paging access into a list of rows
 * sorted by the submitTime column.
 *
 * <p>The table being paged must have the following columns defined: submitTime, id.
 *
 * <p>** If you wish to sort by a different column, create a new paging spec.
 *
 * <p>This spec is recommended for paging in an CSV/JSON export method, where every page in the
 * result will be accessed in a sequential manner. Access performance to every page is constant. No
 * items will be missed by this paging spec.
 *
 * <p>The orderBy expression is (submitTime, id) which means that the order will stay stable for
 * multiple applications that have the same submit time. To avoid a performance penaly on these
 * queries an index over (submitTime, id) should exist.
 */
public class SubmitTimeSequentialAccessPaginationSpec extends BasePaginationSpec {

  // Static object helper definitions.
  public static SubmitTimeSequentialAccessPaginationSpec APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC =
      new SubmitTimeSequentialAccessPaginationSpec(Integer.MAX_VALUE, Instant.MAX, Long.MAX_VALUE);

  private final Instant currentSubmitTime;
  private final Long currentRowId;

  public SubmitTimeSequentialAccessPaginationSpec(
      int pageSize, Instant currentSubmitTime, Long currentRowId) {
    super(pageSize);
    this.currentSubmitTime = currentSubmitTime;
    this.currentRowId = currentRowId;
  }

  @Override
  protected <T> Query<T> applyOrderBy(Query<T> query) {
    return query.orderBy("submitTime desc, id desc");
  }

  @Override
  protected <T> Query<T> maybeApplyWhere(Query<T> query) {
    // Date.from(Instant.MAX) is not supported, (overflows). If that is current
    // submit time in the spec, then skip setting a submitTime in the where
    // clause(), since all values in the database should be before the
    // Instant.MAX Date.
    if (this.currentSubmitTime.equals(Instant.MAX)) {
      return query.where().lt("id", this.currentRowId).query();
    }

    // WHERE (submitTime == currentSubmitTime AND id < currentRowId) OR (submitTime <
    // currentSubmitTime)
    return query
        .where()
        .or()
        .and()
        .eq("submitTime", this.currentSubmitTime)
        .lt("id", this.currentRowId)
        .endAnd()
        .lt("submitTime", this.currentSubmitTime)
        .endOr()
        .query();
  }
}
