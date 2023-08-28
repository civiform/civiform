package services.openApi.v2;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import java.util.Optional;

/**
 * https://swagger.io/specification/v2/#security-definitions-object
 *
 * <p>A declaration of the security schemes available to be used in the specification. This does not
 * enforce the security schemes on the operations and only serves to provide the relevant details
 * for each scheme.
 */
@AutoValue
public abstract class SecurityDefinition {
  /**
   * Required.
   *
   * <p>The type of the security scheme.
   */
  public abstract SecurityType getSecurityType();

  public final String getType() {
    return getSecurityType().toString();
  }

  public final String getLabel() {
    return getSecurityType().getLabel();
  }

  /**
   * Required.
   *
   * <p>The name of the header or query parameter to be used.
   */
  public abstract Optional<String> getName();

  /**
   * Required.
   *
   * <p>The location of the API key. Valid values are "query" or "header".
   */
  public abstract Optional<ApiKeyLocation> getIn();

  /** A short description for security scheme. */
  public abstract Optional<String> getDescription();

  public static SecurityDefinition.Builder basicBuilder() {
    return new AutoValue_SecurityDefinition.Builder().setSecurityType(SecurityType.BASIC);
  }

  public static SecurityDefinition.Builder apiKeyBuilder(String name, ApiKeyLocation in) {
    return new AutoValue_SecurityDefinition.Builder()
        .setSecurityType(SecurityType.API_KEY)
        .setName(name)
        .setIn(in);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    protected abstract SecurityDefinition.Builder setSecurityType(SecurityType securityType);

    protected abstract SecurityDefinition.Builder setName(String name);

    protected abstract SecurityDefinition.Builder setIn(ApiKeyLocation in);

    public abstract SecurityDefinition.Builder setDescription(String description);

    abstract SecurityDefinition autoBuild();

    public final SecurityDefinition build() {
      SecurityDefinition model = autoBuild();

      if (model.getSecurityType() == SecurityType.API_KEY) {
        Preconditions.checkState(
            model.getName().isPresent() && model.getIn().isPresent(),
            "When `SecurityType` == `API_KEY` the fields for `name` and `in` are required");
      }

      return model;
    }
  }
}
