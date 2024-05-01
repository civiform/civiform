package repository;

import com.google.auto.value.AutoValue;
import java.util.Optional;

@AutoValue
public abstract class SearchParameters {

  public enum ParamTypes {
    NAME("name"),
    DAY("day"),
    MONTH("month"),
    YEAR("year");

    public final String label;

    private ParamTypes(String label) {
      this.label = label;
    }
  }

  public abstract Optional<String> nameQuery();

  public abstract Optional<String> dayQuery();

  public abstract Optional<String> monthQuery();

  public abstract Optional<String> yearQuery();

  public static SearchParameters.Builder builder() {
    return new AutoValue_SearchParameters.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract SearchParameters.Builder setNameQuery(Optional<String> v);

    public abstract SearchParameters.Builder setDayQuery(Optional<String> v);

    public abstract SearchParameters.Builder setMonthQuery(Optional<String> v);

    public abstract SearchParameters.Builder setYearQuery(Optional<String> v);

    public abstract SearchParameters build();
  }
}
