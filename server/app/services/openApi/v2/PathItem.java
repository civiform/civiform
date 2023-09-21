package services.openApi.v2;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * Describes the operations available on a single path. A Path Item may be empty, due to ACL
 * constraints. The path itself is still exposed to the documentation viewer but they will not know
 * which operations and parameters are available.
 *
 * <p>https://swagger.io/specification/v2/#path-item-object
 */
@AutoValue
public abstract class PathItem {
  /**
   * Allows for an external definition of this path item. The referenced structure MUST be in the
   * format of a Path Item Object. If there are conflicts between the referenced definition and this
   * Path Item's definition, the behavior is undefined.
   */
  public abstract String getRef();

  /** Get all defined operations */
  public abstract ImmutableList<Operation> getOperations();

  /**
   * A list of parameters that are applicable for all the operations described under this path.
   * These parameters can be overridden at the operation level, but cannot be removed there. The
   * list MUST NOT include duplicated parameters. A unique parameter is defined by a combination of
   * a name and location. The list can use the Reference Object to link to parameters that are
   * defined at the Swagger Object's parameters. There can be one "body" parameter at most.
   */
  public abstract ImmutableList<Parameter> getParameters();

  public static PathItem.Builder builder(String ref) {
    return new AutoValue_PathItem.Builder().setRef(ref);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    protected abstract PathItem.Builder setRef(String ref);

    protected abstract ImmutableList.Builder<Parameter> parametersBuilder();

    public PathItem.Builder addParameter(Parameter parameter) {
      parametersBuilder().add(parameter);
      return this;
    }

    protected abstract ImmutableList.Builder<Operation> operationsBuilder();

    public PathItem.Builder addOperation(Operation operation) {
      operationsBuilder().add(operation);
      return this;
    }

    abstract PathItem autoBuild();

    public final PathItem build() {
      PathItem model = autoBuild();

      if (!model.getRef().startsWith("/")) {
        throw new IllegalArgumentException("PathItem `ref` must start with a forward slash");
      }

      return model;
    }
  }
}
