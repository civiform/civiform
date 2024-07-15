package views.admin.programs;

import java.util.Optional;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ProgramApplicationListView_RenderFilterParams
    extends ProgramApplicationListView.RenderFilterParams {

  private final Optional<String> search;

  private final Optional<String> fromDate;

  private final Optional<String> untilDate;

  private final Optional<String> selectedApplicationStatus;

  private AutoValue_ProgramApplicationListView_RenderFilterParams(
      Optional<String> search,
      Optional<String> fromDate,
      Optional<String> untilDate,
      Optional<String> selectedApplicationStatus) {
    this.search = search;
    this.fromDate = fromDate;
    this.untilDate = untilDate;
    this.selectedApplicationStatus = selectedApplicationStatus;
  }

  @Override
  public Optional<String> search() {
    return search;
  }

  @Override
  public Optional<String> fromDate() {
    return fromDate;
  }

  @Override
  public Optional<String> untilDate() {
    return untilDate;
  }

  @Override
  public Optional<String> selectedApplicationStatus() {
    return selectedApplicationStatus;
  }

  @Override
  public String toString() {
    return "RenderFilterParams{"
        + "search="
        + search
        + ", "
        + "fromDate="
        + fromDate
        + ", "
        + "untilDate="
        + untilDate
        + ", "
        + "selectedApplicationStatus="
        + selectedApplicationStatus
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ProgramApplicationListView.RenderFilterParams) {
      ProgramApplicationListView.RenderFilterParams that =
          (ProgramApplicationListView.RenderFilterParams) o;
      return this.search.equals(that.search())
          && this.fromDate.equals(that.fromDate())
          && this.untilDate.equals(that.untilDate())
          && this.selectedApplicationStatus.equals(that.selectedApplicationStatus());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= search.hashCode();
    h$ *= 1000003;
    h$ ^= fromDate.hashCode();
    h$ *= 1000003;
    h$ ^= untilDate.hashCode();
    h$ *= 1000003;
    h$ ^= selectedApplicationStatus.hashCode();
    return h$;
  }

  static final class Builder extends ProgramApplicationListView.RenderFilterParams.Builder {
    private Optional<String> search = Optional.empty();
    private Optional<String> fromDate = Optional.empty();
    private Optional<String> untilDate = Optional.empty();
    private Optional<String> selectedApplicationStatus = Optional.empty();

    Builder() {}

    @Override
    public ProgramApplicationListView.RenderFilterParams.Builder setSearch(
        Optional<String> search) {
      if (search == null) {
        throw new NullPointerException("Null search");
      }
      this.search = search;
      return this;
    }

    @Override
    public ProgramApplicationListView.RenderFilterParams.Builder setFromDate(
        Optional<String> fromDate) {
      if (fromDate == null) {
        throw new NullPointerException("Null fromDate");
      }
      this.fromDate = fromDate;
      return this;
    }

    @Override
    public ProgramApplicationListView.RenderFilterParams.Builder setUntilDate(
        Optional<String> untilDate) {
      if (untilDate == null) {
        throw new NullPointerException("Null untilDate");
      }
      this.untilDate = untilDate;
      return this;
    }

    @Override
    public ProgramApplicationListView.RenderFilterParams.Builder setSelectedApplicationStatus(
        Optional<String> selectedApplicationStatus) {
      if (selectedApplicationStatus == null) {
        throw new NullPointerException("Null selectedApplicationStatus");
      }
      this.selectedApplicationStatus = selectedApplicationStatus;
      return this;
    }

    @Override
    public ProgramApplicationListView.RenderFilterParams build() {
      return new AutoValue_ProgramApplicationListView_RenderFilterParams(
          this.search, this.fromDate, this.untilDate, this.selectedApplicationStatus);
    }
  }
}
