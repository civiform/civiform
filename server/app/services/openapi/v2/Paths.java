package services.openapi.v2;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

@AutoValue
public abstract class Paths {
  public abstract Optional<String> getName();

  public abstract ImmutableList<PathItem> getPathItems();

  public static Paths.Builder builder() {
    return new AutoValue_Paths.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Paths.Builder setName(String name);

    protected abstract ImmutableList.Builder<PathItem> pathItemsBuilder();

    public Paths.Builder addPathItem(PathItem pathItem) {
      pathItemsBuilder().add(pathItem);
      return this;
    }

    public abstract Paths build();
  }
}
