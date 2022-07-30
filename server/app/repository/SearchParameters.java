package repository;

import com.google.auto.value.AutoValue;
import java.util.Optional;

@AutoValue
public abstract class SearchParameters {

  public abstract Optional<String> nameQuery();

  public abstract Optional<String> searchDate();

  public static SearchParameters.Builder builder() {
    return new AutoValue_SearchParameters.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract SearchParameters.Builder setNameQuery(Optional<String> v);

    public abstract SearchParameters.Builder setSearchDate(Optional<String> v);

    public abstract SearchParameters build();
  }
}
